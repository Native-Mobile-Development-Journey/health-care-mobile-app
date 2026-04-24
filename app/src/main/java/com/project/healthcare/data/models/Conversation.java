package com.project.healthcare.data.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Conversation {

    public String id;
    public String patientUid;
    public String doctorName;
    public String lastMessage;
    public String timeLabel;
    public int unreadCount;

    public Conversation() {
        // Needed for Firebase deserialization.
    }

    public Conversation(
            String id,
            String patientUid,
            String doctorName,
            String lastMessage,
            String timeLabel,
            int unreadCount
    ) {
        this.id = id;
        this.patientUid = patientUid;
        this.doctorName = doctorName;
        this.lastMessage = lastMessage;
        this.timeLabel = timeLabel;
        this.unreadCount = unreadCount;
    }
}
