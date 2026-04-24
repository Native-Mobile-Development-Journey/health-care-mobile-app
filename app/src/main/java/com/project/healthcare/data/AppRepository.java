package com.project.healthcare.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.project.healthcare.data.models.Appointment;
import com.project.healthcare.data.models.Conversation;
import com.project.healthcare.data.models.Doctor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private static final String NODE_USERS = "users";
    private static final String NODE_DOCTORS = "doctors";
    private static final String NODE_APPOINTMENTS = "appointments";
    private static final String NODE_CONVERSATIONS = "conversations";

    private static volatile AppRepository instance;

    private final FirebaseAuth auth;
    private final DatabaseReference usersRef;
    private final DatabaseReference doctorsRef;
    private final DatabaseReference appointmentsRef;
    private final DatabaseReference conversationsRef;

    private AppRepository() {
        auth = FirebaseAuth.getInstance();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        usersRef = rootRef.child(NODE_USERS);
        doctorsRef = rootRef.child(NODE_DOCTORS);
        appointmentsRef = rootRef.child(NODE_APPOINTMENTS);
        conversationsRef = rootRef.child(NODE_CONVERSATIONS);
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
