package com.project.healthcare.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.project.healthcare.data.models.Appointment;
import com.project.healthcare.data.models.Conversation;
import com.project.healthcare.data.models.Doctor;
import com.project.healthcare.data.models.DoctorAvailabilitySlot;
import com.project.healthcare.data.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppRepository {

    public interface ListCallback<T> {
        void onData(List<T> items);

        void onError(String message);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    public interface RoleCallback {
        void onRoleLoaded(@Nullable String role);

        void onError(String message);
    }

    public interface DoctorCallback {
        void onDoctorLoaded(@Nullable Doctor doctor);

        void onError(String message);
    }

    public interface ConversationCallback {
        void onConversationLoaded(@Nullable Conversation conversation);

        void onError(String message);
    }

    private static final String NODE_USERS = "users";
    private static final String NODE_DOCTORS = "doctors";
    private static final String NODE_APPOINTMENTS = "appointments";
    private static final String NODE_CONVERSATIONS = "conversations";
    private static final String NODE_CONVERSATION_MESSAGES = "messages";
    private static final String DOCS_COLLECTION = "doctors";
    private static final String APPOINTMENTS_COLLECTION = "appointments";
    private static final String AVAILABILITY_COLLECTION = "availability";
    private static final String PATIENTS_COLLECTION = "patients";

    private static volatile AppRepository instance;

    private final FirebaseAuth auth;
    private final DatabaseReference usersRef;
    private final DatabaseReference doctorsRef;
    private final DatabaseReference appointmentsRef;
    private final DatabaseReference conversationsRef;
    private final FirebaseFirestore firestore;
    private final FirebaseFunctions functions;

    private AppRepository() {
        auth = FirebaseAuth.getInstance();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        usersRef = rootRef.child(NODE_USERS);
        doctorsRef = rootRef.child(NODE_DOCTORS);
        appointmentsRef = rootRef.child(NODE_APPOINTMENTS);
        conversationsRef = rootRef.child(NODE_CONVERSATIONS);
        firestore = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();
    }

    public static AppRepository getInstance() {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    instance = new AppRepository();
                }
            }
        }
        return instance;
    }

    @Nullable
    public String getCurrentUserUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @NonNull
    public String getCurrentUserDisplayName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return "Patient";
        }
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().trim();
        }
        return "Patient";
    }

    public void createOrUpdateUserProfile(String uid, String name, String email) {
        createOrUpdateUserProfile(uid, name, email, null, null);
    }

    public void createOrUpdateUserProfile(String uid, String name, String email, @Nullable String role, @Nullable CompletionCallback callback) {
        Map<String, Object> profile = UserProfileDataUtil.buildProfile(uid, name, email, role);

        DatabaseReference userRef = usersRef.child(uid);
        if (callback == null) {
            userRef.updateChildren(profile);
            return;
        }

        userRef.updateChildren(profile)
                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public void saveUserProfileToFirestore(String uid, String name, String email, @Nullable String role, @Nullable CompletionCallback callback) {
        Map<String, Object> profile = UserProfileDataUtil.buildProfile(uid, name, email, role);
        DocumentReference document = firestore.collection("users").document(uid);
        if (callback == null) {
            document.set(profile);
            return;
        }

        document.set(profile)
                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public void fetchUserRoleFromFirestore(String uid, RoleCallback callback) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onRoleLoaded(null);
                        return;
                    }
                    String role = snapshot.getString("role");
                    if (role != null) {
                        callback.onRoleLoaded(role.trim().toLowerCase(Locale.ROOT));
                    } else {
                        callback.onRoleLoaded(null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void requestRoleClaim(String uid, String role, @Nullable CompletionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("role", role);

        functions.getHttpsCallable("setUserRoleClaim")
                .call(data)
                .addOnSuccessListener((HttpsCallableResult result) -> {
                    if (callback != null) {
                        callback.onComplete(true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onComplete(false, e.getMessage());
                    }
                });
    }

    public void saveDoctorProfile(Doctor doctor, @Nullable CompletionCallback callback) {
        if (doctor == null || doctor.id == null) {
            if (callback != null) {
                callback.onComplete(false, "Invalid doctor data");
            }
            return;
        }

        DocumentReference document = firestore.collection(DOCS_COLLECTION).document(doctor.id);
        Map<String, Object> payload = new HashMap<>();
        payload.put("uid", doctor.id);
        payload.put("name", doctor.name);
        payload.put("specialty", doctor.specialty);
        payload.put("hospital", doctor.hospital);
        payload.put("bio", doctor.bio);
        payload.put("experienceYears", doctor.experienceYears);
        payload.put("patientCount", doctor.patientCount);
        payload.put("rating", doctor.rating);
        payload.put("ratingCount", doctor.ratingCount);
        payload.put("qualification", doctor.qualification);
        payload.put("languages", doctor.languages);
        payload.put("consultationFee", doctor.consultationFee);
        payload.put("address", doctor.address);
        payload.put("photoUrl", doctor.photoUrl);
        payload.put("isAvailable", doctor.isAvailable);
        payload.put("updatedAt", System.currentTimeMillis());

        if (callback == null) {
            document.set(payload);
            return;
        }

        document.set(payload)
                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public void getDoctorProfile(String doctorId, DoctorCallback callback) {
        if (doctorId == null) {
            callback.onDoctorLoaded(null);
            return;
        }

        firestore.collection(DOCS_COLLECTION).document(doctorId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onDoctorLoaded(null);
                        return;
                    }
                    Doctor doctor = snapshot.toObject(Doctor.class);
                    if (doctor != null && (doctor.id == null || doctor.id.isEmpty())) {
                        doctor.id = doctorId;
                    }
                    callback.onDoctorLoaded(doctor);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void fetchAllDoctorProfiles(ListCallback<Doctor> callback) {
        firestore.collection(DOCS_COLLECTION).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Doctor> doctors = new ArrayList<>();
                    querySnapshot.getDocuments().forEach(document -> {
                        Doctor doctor = document.toObject(Doctor.class);
                        if (doctor == null) {
                            return;
                        }
                        if (doctor.id == null || doctor.id.isEmpty()) {
                            doctor.id = document.getId();
                        }
                        doctors.add(doctor);
                    });
                    Collections.shuffle(doctors);
                    callback.onData(doctors);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void saveDoctorAvailabilitySlot(String doctorId, DoctorAvailabilitySlot slot, @Nullable CompletionCallback callback) {
        if (doctorId == null || slot == null) {
            if (callback != null) {
                callback.onComplete(false, "Invalid availability slot");
            }
            return;
        }

        String slotId = slot.id != null && !slot.id.trim().isEmpty() ? slot.id : firestore.collection(DOCS_COLLECTION).document(doctorId).collection(AVAILABILITY_COLLECTION).document().getId();
        slot.id = slotId;
        DocumentReference document = firestore.collection(DOCS_COLLECTION)
                .document(doctorId)
                .collection(AVAILABILITY_COLLECTION)
                .document(slotId);

        document.set(slot)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onComplete(true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onComplete(false, e.getMessage());
                    }
                });
    }

    public void deleteDoctorAvailabilitySlot(String doctorId, String slotId, @Nullable CompletionCallback callback) {
        if (doctorId == null || slotId == null) {
            if (callback != null) {
                callback.onComplete(false, "Invalid slot reference");
            }
            return;
        }

        firestore.collection(DOCS_COLLECTION)
                .document(doctorId)
                .collection(AVAILABILITY_COLLECTION)
                .document(slotId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onComplete(true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onComplete(false, e.getMessage());
                    }
                });
    }

    public void getDoctorAvailability(String doctorId, ListCallback<DoctorAvailabilitySlot> callback) {
        firestore.collection(DOCS_COLLECTION)
                .document(doctorId)
                .collection(AVAILABILITY_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DoctorAvailabilitySlot> slots = new ArrayList<>();
                    querySnapshot.getDocuments().forEach(document -> {
                        DoctorAvailabilitySlot slot = document.toObject(DoctorAvailabilitySlot.class);
                        if (slot != null) {
                            slot.id = document.getId();
                            slots.add(slot);
                        }
                    });
                    callback.onData(slots);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getDoctorAppointments(String doctorId, ListCallback<Appointment> callback) {
        firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo("doctorId", doctorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> appointments = new ArrayList<>();
                    querySnapshot.getDocuments().forEach(document -> {
                        Appointment appointment = document.toObject(Appointment.class);
                        if (appointment != null) {
                            appointment.id = document.getId();
                            appointments.add(appointment);
                        }
                    });
                    Collections.sort(appointments, Comparator.comparing(a -> safeLower(a.date + " " + a.time)));
                    callback.onData(appointments);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void saveDoctorPatientReference(String doctorId, String patientUid, String patientName, String lastAppointmentDate, String status, @Nullable CompletionCallback callback) {
        if (doctorId == null || patientUid == null) {
            if (callback != null) {
                callback.onComplete(false, "Invalid doctor/patient reference");
            }
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("patientUid", patientUid);
        payload.put("patientName", patientName);
        payload.put("lastAppointmentDate", lastAppointmentDate);
        payload.put("lastAppointmentStatus", status);

        firestore.collection(DOCS_COLLECTION)
                .document(doctorId)
                .collection(PATIENTS_COLLECTION)
                .document(patientUid)
                .set(payload)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onComplete(true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onComplete(false, e.getMessage());
                    }
                });
    }

    public void fetchUserRole(String uid, RoleCallback callback) {
        usersRef.child(uid).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onRoleLoaded(UserProfileDataUtil.parseRoleFromSnapshot(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public ValueEventListener observeDoctors(ListCallback<Doctor> callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Doctor> doctors = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Doctor doctor = child.getValue(Doctor.class);
                    if (doctor == null) {
                        continue;
                    }
                    if (doctor.id == null || doctor.id.isEmpty()) {
                        doctor.id = child.getKey();
                    }
                    doctors.add(doctor);
                }
                Collections.sort(doctors, Comparator.comparing(d -> safeLower(d.name)));
                callback.onData(doctors);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
        doctorsRef.addValueEventListener(listener);
        return listener;
    }

    public void removeDoctorsListener(ValueEventListener listener) {
        doctorsRef.removeEventListener(listener);
    }

    public ValueEventListener observeAppointments(String uid, ListCallback<Appointment> callback) {
        Query query = appointmentsRef.orderByChild("patientUid").equalTo(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Appointment> appointments = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment appointment = child.getValue(Appointment.class);
                    if (appointment == null) {
                        continue;
                    }
                    if (appointment.id == null || appointment.id.isEmpty()) {
                        appointment.id = child.getKey();
                    }
                    appointments.add(appointment);
                }
                Collections.sort(appointments, Comparator.comparing(a -> safeLower(a.date + " " + a.time)));
                callback.onData(appointments);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
        query.addValueEventListener(listener);
        return listener;
    }

    public void removeAppointmentsListener(String uid, ValueEventListener listener) {
        appointmentsRef.removeEventListener(listener);
        appointmentsRef.orderByChild("patientUid").equalTo(uid).removeEventListener(listener);
    }

    public ValueEventListener observeConversations(String uid, ListCallback<Conversation> callback) {
        Query query = conversationsRef.orderByChild("patientUid").equalTo(uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Conversation> conversations = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Conversation conversation = child.getValue(Conversation.class);
                    if (conversation == null) {
                        continue;
                    }
                    if (conversation.id == null || conversation.id.isEmpty()) {
                        conversation.id = child.getKey();
                    }
                    conversations.add(conversation);
                }
                Collections.sort(conversations, Comparator.comparing(c -> safeLower(c.doctorName)));
                callback.onData(conversations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
        query.addValueEventListener(listener);
        return listener;
    }

    public void removeConversationsListener(String uid, ValueEventListener listener) {
        conversationsRef.removeEventListener(listener);
        conversationsRef.orderByChild("patientUid").equalTo(uid).removeEventListener(listener);
    }

    public ValueEventListener observeDoctorConversations(String doctorUid, ListCallback<Conversation> callback) {
        Query query = conversationsRef.orderByChild("doctorUid").equalTo(doctorUid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Conversation> conversations = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Conversation conversation = child.getValue(Conversation.class);
                    if (conversation == null) {
                        continue;
                    }
                    if (conversation.id == null || conversation.id.isEmpty()) {
                        conversation.id = child.getKey();
                    }
                    conversations.add(conversation);
                }
                Collections.sort(conversations, Comparator.comparing(c -> safeLower(c.patientName != null ? c.patientName : "")));
                callback.onData(conversations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
        query.addValueEventListener(listener);
        return listener;
    }

    public void removeDoctorConversationsListener(String doctorUid, ValueEventListener listener) {
        conversationsRef.removeEventListener(listener);
        conversationsRef.orderByChild("doctorUid").equalTo(doctorUid).removeEventListener(listener);
    }

    public void findOrCreateConversation(String patientUid, String patientName, String doctorUid, String doctorName, String initialMessage, ConversationCallback callback) {
        if (patientUid == null || doctorUid == null) {
            callback.onError("Invalid conversation participants");
            return;
        }

        Query query = conversationsRef.orderByChild("patientUid").equalTo(patientUid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Conversation conversation = child.getValue(Conversation.class);
                    if (conversation == null || conversation.doctorUid == null) {
                        continue;
                    }
                    if (conversation.doctorUid.equals(doctorUid)) {
                        if (conversation.id == null || conversation.id.isEmpty()) {
                            conversation.id = child.getKey();
                        }
                        callback.onConversationLoaded(conversation);
                        return;
                    }
                }

                String conversationId = conversationsRef.push().getKey();
                if (conversationId == null) {
                    callback.onError("Unable to create conversation");
                    return;
                }

                Conversation conversation = new Conversation(
                        conversationId,
                        patientUid,
                        patientName,
                        doctorUid,
                        doctorName,
                        initialMessage != null ? initialMessage : "",
                        "",
                        0,
                        null
                );
                conversationsRef.child(conversationId).setValue(conversation)
                        .addOnSuccessListener(unused -> callback.onConversationLoaded(conversation))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public ValueEventListener observeConversationMessages(String conversationId, ListCallback<Message> callback) {
        if (conversationId == null) {
            callback.onData(Collections.emptyList());
            return null;
        }

        DatabaseReference messagesRef = conversationsRef.child(conversationId).child(NODE_CONVERSATION_MESSAGES);
        Query query = messagesRef.orderByChild("timestamp");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message message = child.getValue(Message.class);
                    if (message == null) {
                        continue;
                    }
                    if (message.id == null || message.id.isEmpty()) {
                        message.id = child.getKey();
                    }
                    messages.add(message);
                }
                Collections.sort(messages, Comparator.comparingLong(m -> m.timestamp));
                callback.onData(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
        query.addValueEventListener(listener);
        return listener;
    }

    public void removeConversationMessagesListener(String conversationId, ValueEventListener listener) {
        if (conversationId == null || listener == null) {
            return;
        }
        DatabaseReference messagesRef = conversationsRef.child(conversationId).child(NODE_CONVERSATION_MESSAGES);
        messagesRef.removeEventListener(listener);
        messagesRef.orderByChild("timestamp").removeEventListener(listener);
    }

    public void sendMessage(String conversationId, Message message, @Nullable CompletionCallback callback) {
        if (conversationId == null || message == null) {
            if (callback != null) {
                callback.onComplete(false, "Unable to send message");
            }
            return;
        }

        String messageId = conversationsRef.child(conversationId).child(NODE_CONVERSATION_MESSAGES).push().getKey();
        if (messageId == null) {
            if (callback != null) {
                callback.onComplete(false, "Unable to send message");
            }
            return;
        }
        message.id = messageId;
        message.timestamp = System.currentTimeMillis();

        DatabaseReference messageRef = conversationsRef.child(conversationId).child(NODE_CONVERSATION_MESSAGES).child(messageId);
        messageRef.setValue(message).addOnSuccessListener(unused -> {
            Map<String, Object> update = new HashMap<>();
            update.put("lastMessage", message.text);
            update.put("timeLabel", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(message.timestamp)));
            update.put("unreadCount", ServerValue.increment(1));
            conversationsRef.child(conversationId).updateChildren(update);
            if (callback != null) {
                callback.onComplete(true, null);
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onComplete(false, e.getMessage());
            }
        });
    }

    public void createAppointment(Doctor doctor, String date, String time, CompletionCallback callback) {
        String uid = getCurrentUserUid();
        if (uid == null) {
            callback.onComplete(false, "User not logged in");
            return;
        }

        String appointmentId = appointmentsRef.push().getKey();
        if (appointmentId == null) {
            callback.onComplete(false, "Could not create appointment");
            return;
        }

        Appointment appointment = new Appointment(
                appointmentId,
                uid,
                doctor.id,
                doctor.name,
                doctor.specialty,
                doctor.hospital,
                date,
                time,
                Appointment.STATUS_UPCOMING
        );

        appointmentsRef.child(appointmentId)
                .setValue(appointment)
                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public void rescheduleAppointment(String appointmentId, String date, String time, CompletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("date", date);
        updates.put("time", time);
        updates.put("status", Appointment.STATUS_UPCOMING);

        appointmentsRef.child(appointmentId)
                .updateChildren(updates)
                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    @Nullable
    public Doctor findDoctorById(List<Doctor> doctors, String doctorId) {
        if (doctorId == null) {
            return null;
        }
        for (Doctor doctor : doctors) {
            if (doctorId.equals(doctor.id)) {
                return doctor;
            }
        }
        return null;
    }

    private String safeLower(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
