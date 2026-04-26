package com.project.healthcare.data.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DoctorAvailabilitySlot {

    public String id;
    public String dayOfWeek;
    public String dayLabel;
    public String startTime;      // legacy display string (kept for compatibility)
    public String endTime;        // legacy display string (kept for compatibility)
    public int durationMinutes;
    public boolean isActive;
    public long startMinuteOfDay; // NEW: minutes from midnight (0..1439) for overlap checks
    public long endMinuteOfDay;   // NEW: minutes from midnight (exclusive)
    public String timeZoneId;     // NEW: canonical timezone for this slot template
    public long capacity;         // NEW: max bookings allowed per slot (default 1)
    public long bookedCount;      // NEW: current bookings for this slot template instance
    public String slotKey;        // NEW: stable key for locking (e.g. "YYYYMMDD-HHMM-doctorId")

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

    // Compute canonical slotKey from dayLabel + startTime + doctorId (stable for locking).
    public String computeSlotKey(String doctorId) {
        if (doctorId == null || dayLabel == null || startTime == null) {
            return null;
        }
        // Normalize dayLabel to sortable prefix: Mon -> 1, Tue -> 2, ...
        int dow = dayLabelToNumber(dayLabel);
        if (dow < 0) return doctorId + "-" + dayLabel + "-" + startTime;
        // Parse startTime "hh:mm AM/PM" into minutes since midnight
        int mins = parseTimeToMinutes(startTime);
        if (mins < 0) return doctorId + "-" + dayLabel + "-" + startTime;
        // Format: DDMM-HHMM-doctorId (using a stable numeric day-of-week + minutes)
        return String.format(java.util.Locale.US, "%02d%02d-%04d-%s", dow, mins / 60, mins % 60, doctorId);
    }

    // Parse "hh:mm AM/PM" into minutes since midnight. Returns -1 on failure.
    public static int parseTimeToMinutes(String time) {
        if (time == null || time.trim().isEmpty()) return -1;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US);
            java.util.Date d = sdf.parse(time.trim());
            if (d == null) return -1;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(d);
            return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
        } catch (Exception e) {
            return -1;
        }
    }

    // Convert localized day label to stable number (Mon=1..Sun=7). Returns -1 if unknown.
    private static int dayLabelToNumber(String label) {
        if (label == null) return -1;
        String l = label.trim().toLowerCase(java.util.Locale.US);
        switch (l) {
            case "mon": case "monday": return 1;
            case "tue": case "tuesday": return 2;
            case "wed": case "wednesday": return 3;
            case "thu": case "thursday": return 4;
            case "fri": case "friday": return 5;
            case "sat": case "saturday": return 6;
            case "sun": case "sunday": return 7;
            default:
                // try parse leading number
                try { return Integer.parseInt(l); } catch (Exception ignored) {}
                return -1;
        }
    }

    // Compute start/end minute-of-day from legacy strings. Returns true if valid.
    public boolean computeMinutesFromLegacy() {
        int s = parseTimeToMinutes(startTime);
        int e = parseTimeToMinutes(endTime);
        if (s < 0 || e < 0) return false;
        this.startMinuteOfDay = s;
        this.endMinuteOfDay = e;
        if (this.capacity <= 0) this.capacity = 1;
        if (this.timeZoneId == null) this.timeZoneId = java.util.TimeZone.getDefault().getID();
        if (this.slotKey == null) this.slotKey = computeSlotKey(null);
        return true;
    }

    // Upgrade legacy slot documents by computing canonical fields if missing.
    // Call this after deserialization from Firestore to ensure fields are populated.
    public void upgradeLegacyFields(String doctorId) {
        if (this.startMinuteOfDay <= 0 && this.endMinuteOfDay <= 0) {
            computeMinutesFromLegacy();
        }
        if (this.timeZoneId == null) {
            this.timeZoneId = java.util.TimeZone.getDefault().getID();
        }
        if (this.capacity <= 0) {
            this.capacity = 1;
        }
        if (this.slotKey == null || this.slotKey.isEmpty()) {
            this.slotKey = computeSlotKey(doctorId);
        }
        if (this.durationMinutes <= 0) {
            this.durationMinutes = (int) (endMinuteOfDay - startMinuteOfDay);
        }
    }
}
