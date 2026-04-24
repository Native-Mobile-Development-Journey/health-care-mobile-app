package com.project.healthcare.data.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DoctorAvailabilitySlot {

    public String id;
    public String dayOfWeek;
    public String dayLabel;
    public String startTime;
    public String endTime;
    public int durationMinutes;
    public boolean isActive;

    public DoctorAvailabilitySlot() {
        // Needed for Firestore deserialization.
    }

    public DoctorAvailabilitySlot(String id, String dayOfWeek, String dayLabel, String startTime, String endTime, int durationMinutes, boolean isActive) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.dayLabel = dayLabel;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.isActive = isActive;
    }
}
