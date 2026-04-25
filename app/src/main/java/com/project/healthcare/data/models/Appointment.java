package com.project.healthcare.data.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Appointment {

    public static final String STATUS_UPCOMING = "Upcoming";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELED = "Cancel";

    public String id;
    public String patientUid;
    public String doctorId;
    public String doctorName;
    public String specialty;
    public String hospital;
    public String date;
    public String time;
    public String status;
    public String patientName;

    public Appointment() {
        // Needed for Firebase deserialization.
    }

    public Appointment(
            String id,
            String patientUid,
            String doctorId,
            String doctorName,
            String specialty,
            String hospital,
            String date,
            String time,
            String status,
            String patientName
    ) {
        this.id = id;
        this.patientUid = patientUid;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.hospital = hospital;
        this.date = date;
        this.time = time;
        this.status = status;
        this.patientName = patientName;
    }

    public String normalizedStatus() {
        if (status == null) {
            return STATUS_UPCOMING;
        }
        if (status.equalsIgnoreCase(STATUS_COMPLETED)) {
            return STATUS_COMPLETED;
        }
        if (status.equalsIgnoreCase("Cancelled") || status.equalsIgnoreCase("Canceled") || status.equalsIgnoreCase(STATUS_CANCELED)) {
            return STATUS_CANCELED;
        }
        return STATUS_UPCOMING;
    }
}
