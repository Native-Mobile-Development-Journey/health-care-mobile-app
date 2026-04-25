package com.project.healthcare.data.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {

    public String id;
    public String senderUid;
    public String senderName;
    public String text;
    public long timestamp;
    public boolean read;

    public Message() {
        // Needed for Firebase deserialization.
    }

    public Message(
            String id,
            String senderUid,
            String senderName,
            String text,
            long timestamp,
            boolean read
    ) {
        this.id = id;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
        this.read = read;
    }
}
