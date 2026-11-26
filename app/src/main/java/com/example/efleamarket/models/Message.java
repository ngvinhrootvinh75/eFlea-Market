package com.example.efleamarket.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Message {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_VIDEO = "video";

    @DocumentId private String id;
    private String senderId;
    private String text;
    private String type;
    private String mediaUrl;
    @ServerTimestamp private Date createdAt;

    public Message() {}

    @Exclude public boolean isImage() { return TYPE_IMAGE.equals(type); }
    @Exclude public boolean isVideo() { return TYPE_VIDEO.equals(type); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String v) { senderId = v; }
    public String getText() { return text; }
    public void setText(String v) { text = v; }
    public String getType() { return type == null ? TYPE_TEXT : type; }
    public void setType(String v) { type = v; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String v) { mediaUrl = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { createdAt = v; }
}