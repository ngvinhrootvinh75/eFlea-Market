package com.example.efleamarket.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class Transaction {
    public static final String STATUS_OFFERED = "offered";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_SCHEDULED = "scheduled";
    public static final String STATUS_COMPLETED = "completed";

    @DocumentId private String id;
    private String chatId, productId, productTitle, productImage;
    private double listPrice, offeredPrice;
    private String note;
    private String buyerId, sellerId;
    private List<String> participants;
    private String offeredBy;
    private String lastActionBy;
    private String status;
    @ServerTimestamp private Date createdAt;
    @ServerTimestamp private Date updatedAt;

    public Transaction() {}

    @Exclude public boolean isActive() {
        return STATUS_OFFERED.equals(status) || STATUS_ACCEPTED.equals(status) || STATUS_SCHEDULED.equals(status);
    }

    @Exclude public boolean isOfferedBy(String uid) {
        return uid != null && uid.equals(offeredBy);
    }

    @Exclude public boolean isParticipant(String uid) {
        return uid != null && participants != null && participants.contains(uid);
    }

    @Exclude public boolean canRespond(String uid) {
        return STATUS_OFFERED.equals(status) && isParticipant(uid) && !uid.equals(offeredBy);
    }

    @Exclude public boolean canCancel(String uid) {
        if (!isParticipant(uid)) return false;
        if (STATUS_OFFERED.equals(status)) return uid.equals(offeredBy);
        return STATUS_ACCEPTED.equals(status);
    }

    @Exclude public boolean canReOffer(String uid) {
        return isParticipant(uid) && (STATUS_REJECTED.equals(status) || STATUS_CANCELLED.equals(status));
    }

    @Exclude public String getStatusLabel() {
        if (status == null) return "";
        switch (status) {
            case STATUS_OFFERED: return "Đang chờ phản hồi";
            case STATUS_ACCEPTED: return "Đã chấp nhận";
            case STATUS_REJECTED: return "Đã từ chối";
            case STATUS_CANCELLED: return "Đã hủy";
            case STATUS_SCHEDULED: return "Đã hẹn gặp";
            case STATUS_COMPLETED: return "Hoàn tất";
            default: return status;
        }
    }

    @Exclude public int getStatusTextColor() {
        if (status == null) return 0xFF334155;
        switch (status) {
            case STATUS_OFFERED: return 0xFFB45309;
            case STATUS_ACCEPTED: return 0xFF15803D;
            case STATUS_REJECTED:
            case STATUS_CANCELLED: return 0xFFB91C1C;
            case STATUS_SCHEDULED: return 0xFF1D4ED8;
            case STATUS_COMPLETED: return 0xFF0F766E;
            default: return 0xFF334155;
        }
    }

    @Exclude public int getStatusBgColor() {
        if (status == null) return 0xFFF1F5F9;
        switch (status) {
            case STATUS_OFFERED: return 0xFFFEF3C7;
            case STATUS_ACCEPTED: return 0xFFDCFCE7;
            case STATUS_REJECTED:
            case STATUS_CANCELLED: return 0xFFFEE2E2;
            case STATUS_SCHEDULED: return 0xFFDBEAFE;
            case STATUS_COMPLETED: return 0xFFE2F4EB;
            default: return 0xFFF1F5F9;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChatId() { return chatId; }
    public void setChatId(String v) { chatId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String v) { productTitle = v; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String v) { productImage = v; }
    public double getListPrice() { return listPrice; }
    public void setListPrice(double v) { listPrice = v; }
    public double getOfferedPrice() { return offeredPrice; }
    public void setOfferedPrice(double v) { offeredPrice = v; }
    public String getNote() { return note; }
    public void setNote(String v) { note = v; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String v) { buyerId = v; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String v) { sellerId = v; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> v) { participants = v; }
    public String getOfferedBy() { return offeredBy; }
    public void setOfferedBy(String v) { offeredBy = v; }
    public String getLastActionBy() { return lastActionBy; }
    public void setLastActionBy(String v) { lastActionBy = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { createdAt = v; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date v) { updatedAt = v; }
}