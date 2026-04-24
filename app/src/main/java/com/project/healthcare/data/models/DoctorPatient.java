package com.project.healthcare.data.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DoctorPatient {

    public String patientUid;
    public String patientName;
    public String lastAppointmentDate;
    public String lastAppointmentStatus;

    public DoctorPatient() {
        // Needed for Firestore deserialization.
    }

    public DoctorPatient(String patientUid, String patientName, String lastAppointmentDate, String lastAppointmentStatus) {
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.lastAppointmentDate = lastAppointmentDate;
        this.lastAppointmentStatus = lastAppointmentStatus;
    }
}
