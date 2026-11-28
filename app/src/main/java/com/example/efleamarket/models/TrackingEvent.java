package com.example.efleamarket.models;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

public class TrackingEvent {
    @DocumentId private String id;
    private String text, by;
    private Date createdAt;

    public TrackingEvent() {}

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getText() { return text; }
    public void setText(String v) { text = v; }
    public String getBy() { return by; }
    public void setBy(String v) { by = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { createdAt = v; }
}