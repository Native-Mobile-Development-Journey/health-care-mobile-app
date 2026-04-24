const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

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

    const userDoc = admin.firestore().collection("users").doc(uid);
    await userDoc.set({ role }, { merge: true });

    return { success: true, role };
});
