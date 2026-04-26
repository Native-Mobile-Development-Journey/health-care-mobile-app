const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const validRoles = ["doctor", "patient"];

exports.setUserRoleClaim = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication is required.");
    }

    const uid = data.uid;
    const role = typeof data.role === "string" ? data.role.trim().toLowerCase() : null;

    if (!uid || typeof uid !== "string") {
        throw new functions.https.HttpsError("invalid-argument", "A valid uid is required.");
    }

    if (!role || !validRoles.includes(role)) {
        throw new functions.https.HttpsError("invalid-argument", "Role must be either 'doctor' or 'patient'.");
    }

    if (context.auth.uid !== uid) {
        throw new functions.https.HttpsError("permission-denied", "You may only update your own role.");
    }

    await admin.auth().setCustomUserClaims(uid, { role });

    const userDoc = db.collection("users").doc(uid);
    await userDoc.set({ role }, { merge: true });

    return { success: true, role };
});

// Scheduled function to expire unresolved appointments older than 7 days and release slot locks.
// Runs daily at 02:00 UTC.
exports.expireOldAppointments = functions.pubsub
    .schedule("0 2 * * *")
    .timeZone("UTC")
    .onRun(async (context) => {
        const now = admin.firestore.Timestamp.now();
        const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;
        const cutoffTime = new Date(now.toDate().getTime() - sevenDaysMs);

        console.log(`Running expiry job at ${now.toDate().toISOString()}, cutoff=${cutoffTime.toISOString()}`);

        const appointmentsRef = db.collection("appointments");
        const batchSize = 500;

        try {
            // Find UPCOMING appointments where startAt is older than 7 days (or where legacy date/time indicates old)
            const query = appointmentsRef
                .where("status", "==", "Upcoming")
                .where("startAt", "<=", cutoffTime)
                .limit(batchSize);

            const snapshot = await query.get();
            if (snapshot.empty) {
                console.log("No expired appointments found.");
                return null;
            }

            const batch = db.batch();
            let count = 0;

            snapshot.docs.forEach((doc) => {
                const appt = doc.data();
                // Mark as Expired
                batch.update(doc.ref, {
                    status: "Expired",
                    updatedAt: now,
                    statusChangedBy: "system",
                    statusChangedAt: now,
                });

                // Release slot lock if slotKey exists: decrement bookedCount on the slot document
                if (appt.slotKey) {
                    // Try to find and update the slot document in doctors/{doctorId}/availability/{slotId}
                    // We don't know exact slotId from appointment, but we can search by slotKey in indexes.
                    // Simpler: store slot document path in appointment (slotRef) or do best-effort update via collection group.
                    // For now, we'll attempt to update any availability document with matching slotKey under the doctor.
                    const slotUpdateRef = db.collection("doctors")
                        .doc(appt.doctorId)
                        .collection("availability")
                        .where("slotKey", "==", appt.slotKey)
                        .limit(1);
                    // We can't batch update a query directly; we'll do a separate transaction per slot or accept eventual consistency.
                    // Instead, we'll store slotDocId in appointment to make this atomic. For now, log and handle separately.
                    console.log(`Appointment ${doc.id} has slotKey=${appt.slotKey} but slotDocId not stored; skipping lock release in this run.`);
                }

                count++;
            });

            if (count > 0) {
                await batch.commit();
                console.log(`Expired ${count} appointments.`);
            }

            // TODO: Add a follow-up process to release slot locks by storing slotDocId in appointment or using a transaction per slot.
            return { expired: count };
        } catch (error) {
            console.error("Error expiring appointments:", error);
            throw error;
        }
    });

// Optional: callable function to create appointment with atomic slot lock (recommended for production).
// This would replace client-side createAppointment and enforce all checks server-side.
exports.cancelAppointment = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
    }

    const { appointmentId } = data;
    const requesterUid = context.auth.uid;

    if (!appointmentId) {
        throw new functions.https.HttpsError("invalid-argument", "Appointment ID required.");
    }

    const now = admin.firestore.Timestamp.now();

    return db.runTransaction(async (transaction) => {
        const appointmentRef = db.collection("appointments").doc(appointmentId);
        const appointmentSnap = await transaction.get(appointmentRef);

        if (!appointmentSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Appointment not found.");
        }

        const appointment = appointmentSnap.data();

        // Authorization: only patient or doctor can cancel
        if (appointment.patientUid !== requesterUid && appointment.doctorId !== requesterUid) {
            throw new functions.https.HttpsError("permission-denied", "Not authorized to cancel this appointment.");
        }

        // Validate state transition
        if (appointment.status !== "Upcoming") {
            throw new functions.https.HttpsError("failed-precondition", "Only upcoming appointments can be cancelled.");
        }

        // Update appointment status
        transaction.update(appointmentRef, {
            status: "Canceled",
            updatedAt: now,
            statusChangedAt: now,
            statusChangedBy: requesterUid
        });

        // Release slot lock if slotDocId exists
        if (appointment.slotDocId) {
            const slotRef = db.collection("doctors")
                .doc(appointment.doctorId)
                .collection("availability")
                .doc(appointment.slotDocId);

            transaction.update(slotRef, {
                bookedCount: admin.firestore.FieldValue.increment(-1),
                updatedAt: now
            });
        }

        return { success: true };
    });
});

exports.rescheduleAppointment = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
    }

    const { appointmentId, newSlotDocId, newSlotKey, newStartAt, newEndAt, newDate, newTime } = data;
    const requesterUid = context.auth.uid;

    if (!appointmentId || !newSlotDocId || !newSlotKey || !newStartAt || !newEndAt) {
        throw new functions.https.HttpsError("invalid-argument", "Missing required fields.");
    }

    const now = admin.firestore.Timestamp.now();

    return db.runTransaction(async (transaction) => {
        // 1. Get existing appointment
        const appointmentRef = db.collection("appointments").doc(appointmentId);
        const appointmentSnap = await transaction.get(appointmentRef);

        if (!appointmentSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Appointment not found.");
        }

        const appointment = appointmentSnap.data();

        // Authorization
        if (appointment.patientUid !== requesterUid && appointment.doctorId !== requesterUid) {
            throw new functions.https.HttpsError("permission-denied", "Not authorized to reschedule this appointment.");
        }

        // Only upcoming appointments can be rescheduled
        if (appointment.status !== "Upcoming") {
            throw new functions.https.HttpsError("failed-precondition", "Only upcoming appointments can be rescheduled.");
        }

        // 2. Release old slot lock
        if (appointment.slotDocId) {
            const oldSlotRef = db.collection("doctors")
                .doc(appointment.doctorId)
                .collection("availability")
                .doc(appointment.slotDocId);

            transaction.update(oldSlotRef, {
                bookedCount: admin.firestore.FieldValue.increment(-1),
                updatedAt: now
            });
        }

        // 3. Lock new slot
        const newSlotRef = db.collection("doctors")
            .doc(appointment.doctorId)
            .collection("availability")
            .doc(newSlotDocId);

        const newSlotSnap = await transaction.get(newSlotRef);

        if (!newSlotSnap.exists) {
            throw new functions.https.HttpsError("not-found", "New slot no longer exists.");
        }

        const newSlot = newSlotSnap.data();

        if (!newSlot.isActive || newSlot.bookedCount >= newSlot.capacity) {
            throw new functions.https.HttpsError("resource-exhausted", "This slot is no longer available.");
        }

        // 4. Check for conflicts in new time window
        const doctorConflicts = await transaction.get(
            db.collection("appointments")
                .where("doctorId", "==", appointment.doctorId)
                .where("status", "==", "Upcoming")
                .where("startAt", "<", newEndAt)
                .where("endAt", ">", newStartAt)
                .limit(1)
        );

        if (!doctorConflicts.empty && doctorConflicts.docs[0].id !== appointmentId) {
            throw new functions.https.HttpsError("already-exists", "Doctor has conflicting appointment for new time.");
        }

        // 5. Update appointment
        transaction.update(appointmentRef, {
            slotDocId: newSlotDocId,
            slotKey: newSlotKey,
            startAt: newStartAt,
            endAt: newEndAt,
            date: newDate,
            time: newTime,
            updatedAt: now,
            statusChangedAt: now,
            statusChangedBy: requesterUid
        });

        // 6. Reserve new slot
        transaction.update(newSlotRef, {
            bookedCount: admin.firestore.FieldValue.increment(1),
            updatedAt: now
        });

        return { success: true, appointmentId };
    });
});

exports.createAppointmentWithLock = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
    }

    const { doctorId, slotDocId, slotKey, startAt, endAt, timeZoneId, date, time, idempotencyKey, doctorName, patientName, specialty, hospital } = data;
    const patientUid = context.auth.uid;

    // Validate required fields
    if (!doctorId || !slotDocId || !slotKey || !startAt || !endAt) {
        throw new functions.https.HttpsError("invalid-argument", "Missing required appointment fields.");
    }

    if (startAt >= endAt) {
        throw new functions.https.HttpsError("invalid-argument", "Start time must be before end time.");
    }

    const now = admin.firestore.Timestamp.now();

    // Run atomic transaction for booking
    const result = await db.runTransaction(async (transaction) => {
        // 1. Get slot document and verify availability
        const slotRef = db.collection("doctors").doc(doctorId).collection("availability").doc(slotDocId);
        const slotSnap = await transaction.get(slotRef);

        if (!slotSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Slot no longer exists.");
        }

        const slot = slotSnap.data();
        
        if (!slot.isActive) {
            throw new functions.https.HttpsError("failed-precondition", "Slot is no longer available.");
        }

        if (slot.bookedCount >= slot.capacity) {
            throw new functions.https.HttpsError("resource-exhausted", "This slot is already fully booked.");
        }

        // 2. Check doctor has no conflicting appointments for this time window
        const doctorConflictsQuery = db.collection("appointments")
            .where("doctorId", "==", doctorId)
            .where("status", "==", "Upcoming")
            .where("startAt", "<", endAt)
            .where("endAt", ">", startAt)
            .limit(1);
        
        const doctorConflicts = await transaction.get(doctorConflictsQuery);
        if (!doctorConflicts.empty) {
            throw new functions.https.HttpsError("already-exists", "Doctor has conflicting appointment for this time.");
        }

        // 3. Check patient has no conflicting appointments for this time window
        const patientConflictsQuery = db.collection("appointments")
            .where("patientUid", "==", patientUid)
            .where("status", "==", "Upcoming")
            .where("startAt", "<", endAt)
            .where("endAt", ">", startAt)
            .limit(1);
        
        const patientConflicts = await transaction.get(patientConflictsQuery);
        if (!patientConflicts.empty) {
            throw new functions.https.HttpsError("already-exists", "You already have an appointment for this time.");
        }

        // 4. Create new appointment document
        const appointmentRef = db.collection("appointments").doc();
        const appointment = {
            id: appointmentRef.id,
            doctorId,
            patientUid,
            slotKey,
            slotDocId,
            startAt,
            endAt,
            timeZoneId,
            date,
            time,
            doctorName,
            patientName,
            specialty,
            hospital,
            status: "Upcoming",
            createdAt: now,
            updatedAt: now,
            statusChangedAt: now,
            statusChangedBy: patientUid,
            idempotencyKey
        };

        transaction.create(appointmentRef, appointment);

        // 5. Atomically increment slot booked count
        transaction.update(slotRef, {
            bookedCount: admin.firestore.FieldValue.increment(1),
            updatedAt: now
        });

        // 6. Upsert doctor-patient reference
        const patientRef = db.collection("doctors").doc(doctorId).collection("patients").doc(patientUid);
        transaction.set(patientRef, {
            patientUid,
            patientName,
            lastAppointmentDate: date,
            lastAppointmentStartAt: startAt,
            lastAppointmentStatus: "Upcoming",
            updatedAt: now
        }, { merge: true });

        return {
            appointmentId: appointmentRef.id,
            success: true
        };
    });

    return result;
});
