package com.project.healthcare.data.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class Conversation {

    public String id;
    public String patientUid;
    public String patientName;
    public String doctorUid;
    public String doctorName;
    public String lastMessage;
    public String timeLabel;
    public int unreadCount;
    public List<Message> messages;

    public Conversation() {
        // Needed for Firebase deserialization.
    }

    public Conversation(
            String id,
            String patientUid,
            String patientName,
            String doctorUid,
            String doctorName,
            String lastMessage,
            String timeLabel,
            int unreadCount,
            List<Message> messages
    ) {
        this.id = id;
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.doctorUid = doctorUid;
        this.doctorName = doctorName;
        this.lastMessage = lastMessage;
        this.timeLabel = timeLabel;
        this.unreadCount = unreadCount;
        this.messages = messages;
    }
}
