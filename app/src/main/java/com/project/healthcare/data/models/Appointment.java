package com.project.healthcare.data.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Appointment {

    public static final String STATUS_UPCOMING = "Upcoming";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELED = "Canceled";
    public static final String STATUS_EXPIRED = "Expired";
    public static final String STATUS_NO_SHOW = "NoShow";

    public String id;
    public String patientUid;
    public String doctorId;
    public String doctorName;
    public String specialty;
    public String hospital;
    public String date;          // legacy string (kept for compatibility)
    public String time;          // legacy string (kept for compatibility)
    public long startAt;         // canonical start timestamp (epoch ms, UTC)
    public long endAt;           // canonical end timestamp (epoch ms, UTC)
    public String timeZoneId;    // canonical timezone for display (e.g. "America/New_York")
    public String slotKey;       // stable lock key for this appointment slot
    public String status;
    public String patientName;
    public long createdAt;       // when appointment was created (epoch ms)
    public long updatedAt;       // last status/timestamp update (epoch ms)
    public long statusChangedAt; // when status was last changed (epoch ms)
    public String statusChangedBy; // who last changed status (uid or "system")

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
            long startAt,
            long endAt,
            String timeZoneId,
            String status,
            String patientName,
            long createdAt,
            long updatedAt,
            String statusChangedBy
    ) {
        this.id = id;
        this.patientUid = patientUid;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.hospital = hospital;
        this.date = date;
        this.time = time;
        this.startAt = startAt;
        this.endAt = endAt;
        this.timeZoneId = timeZoneId;
        this.status = status;
        this.patientName = patientName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statusChangedBy = statusChangedBy;
    }

    /**
     * Check if this appointment is in a terminal state (cannot return to UPCOMING)
     */
    public boolean isTerminalState() {
        return STATUS_COMPLETED.equals(status)
                || STATUS_CANCELED.equals(status)
                || STATUS_EXPIRED.equals(status)
                || STATUS_NO_SHOW.equals(status);
    }

    /**
     * Validate if status transition is allowed according to lifecycle rules
     * @param newStatus Target status to transition to
     * @return true if transition is valid
     */
    public boolean canTransitionTo(String newStatus) {
        if (newStatus == null || newStatus.isEmpty()) return false;
        
        // Cannot change state of terminal appointments
        if (isTerminalState()) return false;

        // Allowed transitions from UPCOMING
        if (STATUS_UPCOMING.equals(status)) {
            return STATUS_COMPLETED.equals(newStatus)
                    || STATUS_CANCELED.equals(newStatus)
                    || STATUS_EXPIRED.equals(newStatus)
                    || STATUS_NO_SHOW.equals(newStatus);
        }

        return false;
    }

    /**
     * Check if appointment is eligible for automatic expiry (7 days after start time)
     */
    public boolean isOverdueForExpiry() {
        if (!STATUS_UPCOMING.equals(status)) return false;
        if (startAt <= 0) return false;
        
        long sevenDaysMs = 7 * 24 * 60 * 60 * 1000L;
        long expiryThreshold = startAt + sevenDaysMs;
        
        return System.currentTimeMillis() > expiryThreshold;
    }

    /**
     * Normalize status for UI filtering (backwards compatibility)
     */
    public String normalizedStatus() {
        if (status == null) return STATUS_UPCOMING;
        
        // Map legacy status values to current constants
        switch (status.trim().toLowerCase()) {
            case "cancel":
            case "cancelled":
            case "canceled":
                return STATUS_CANCELED;
            case "complete":
            case "completed":
                return STATUS_COMPLETED;
            case "expired":
                return STATUS_EXPIRED;
            case "noshow":
            case "no_show":
            case "no show":
                return STATUS_NO_SHOW;
            case "upcoming":
            default:
                return STATUS_UPCOMING;
        }
    }

}
