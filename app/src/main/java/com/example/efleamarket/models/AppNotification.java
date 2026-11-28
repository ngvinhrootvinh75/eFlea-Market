package com.example.efleamarket.models;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

public class AppNotification {
    @DocumentId private String id;
    private String type, title, body, chatId, fromUid;
    private boolean read;
    private Date createdAt;

    public AppNotification() {}

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getBody() { return body; }
    public void setBody(String v) { body = v; }
    public String getChatId() { return chatId; }
    public void setChatId(String v) { chatId = v; }
    public String getFromUid() { return fromUid; }
    public void setFromUid(String v) { fromUid = v; }
    public boolean isRead() { return read; }
    public void setRead(boolean v) { read = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { createdAt = v; }
}