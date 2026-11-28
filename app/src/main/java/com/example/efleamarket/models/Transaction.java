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
    public static final String STATUS_SHIPPING = "shipping";
    public static final String STATUS_COMPLETED = "completed";

    public static final String DELIVERY_MEETUP = "meetup";
    public static final String DELIVERY_SHIPPING = "shipping";

    public static final String SHIP_WAITING = "waiting";
    public static final String SHIP_SHIPPED = "shipped";
    public static final String SHIP_DELIVERING = "delivering";
    public static final String SHIP_RECEIVED = "received";

    @DocumentId private String id;
    private String chatId, productId, productTitle, productImage;
    private double listPrice, offeredPrice;
    private String note;
    private String buyerId, sellerId;
    private List<String> participants;
    private String offeredBy;
    private String lastActionBy;
    private String status;
    private String deliveryMethod;
    private MeetingPlace meetingPlace;
    private Date meetingTime;
    private ShippingAddress shippingAddress;
    private String shipStep, carrier, trackingCode;
    private Date shipRequestedAt, shippedAt, deliveringAt, receivedAt;
    @ServerTimestamp private Date createdAt;
    @ServerTimestamp private Date updatedAt;

    public Transaction() {}

    @Exclude public boolean isActive() {
        return STATUS_OFFERED.equals(status) || STATUS_ACCEPTED.equals(status)
                || STATUS_SCHEDULED.equals(status) || STATUS_SHIPPING.equals(status);
    }

    @Exclude public boolean isOfferedBy(String uid) { return uid != null && uid.equals(offeredBy); }

    @Exclude public boolean isParticipant(String uid) {
        return uid != null && participants != null && participants.contains(uid);
    }

    @Exclude public boolean isBuyer(String uid) { return uid != null && uid.equals(buyerId); }

    @Exclude public boolean isSeller(String uid) { return uid != null && uid.equals(sellerId); }

    @Exclude public boolean canRespond(String uid) {
        return STATUS_OFFERED.equals(status) && isParticipant(uid) && !uid.equals(offeredBy);
    }

    @Exclude public boolean canCancel(String uid) {
        if (!isParticipant(uid)) return false;
        if (STATUS_OFFERED.equals(status)) return uid.equals(offeredBy);
        if (STATUS_SHIPPING.equals(status)) return SHIP_WAITING.equals(shipStep);
        return STATUS_ACCEPTED.equals(status) || STATUS_SCHEDULED.equals(status);
    }

    @Exclude public boolean canReOffer(String uid) {
        return isParticipant(uid) && (STATUS_REJECTED.equals(status) || STATUS_CANCELLED.equals(status));
    }

    @Exclude public boolean canSchedule(String uid) {
        return isParticipant(uid) && (STATUS_ACCEPTED.equals(status) || STATUS_SCHEDULED.equals(status));
    }

    @Exclude public boolean canRequestShipping(String uid) {
        return STATUS_ACCEPTED.equals(status) && isBuyer(uid);
    }

    @Exclude public boolean canEnterTracking(String uid) {
        return STATUS_SHIPPING.equals(status) && SHIP_WAITING.equals(shipStep) && isSeller(uid);
    }

    @Exclude public boolean canMarkDelivering(String uid) {
        return STATUS_SHIPPING.equals(status) && SHIP_SHIPPED.equals(shipStep) && isSeller(uid);
    }

    @Exclude public boolean canConfirmReceived(String uid) {
        return STATUS_SHIPPING.equals(status)
                && (SHIP_SHIPPED.equals(shipStep) || SHIP_DELIVERING.equals(shipStep))
                && isBuyer(uid);
    }

    @Exclude public boolean canAddTracking(String uid) {
        return STATUS_SHIPPING.equals(status)
                && (SHIP_SHIPPED.equals(shipStep) || SHIP_DELIVERING.equals(shipStep))
                && isSeller(uid);
    }

    @Exclude public boolean hasMeeting() {
        return STATUS_SCHEDULED.equals(status) && meetingPlace != null && meetingTime != null;
    }

    @Exclude public boolean isShippingFlow() {
        return DELIVERY_SHIPPING.equals(deliveryMethod)
                && (STATUS_SHIPPING.equals(status) || STATUS_COMPLETED.equals(status));
    }

    @Exclude public boolean hasTrackingCode() {
        return trackingCode != null && !trackingCode.isEmpty();
    }

    @Exclude public int getShipStepIndex() {
        if (shipStep == null) return -1;
        switch (shipStep) {
            case SHIP_WAITING: return 0;
            case SHIP_SHIPPED: return 1;
            case SHIP_DELIVERING: return 2;
            case SHIP_RECEIVED: return 3;
            default: return -1;
        }
    }

    @Exclude public String getShipStepLabel() {
        if (shipStep == null) return "";
        switch (shipStep) {
            case SHIP_WAITING: return "Chờ người bán gửi hàng";
            case SHIP_SHIPPED: return "Đã gửi hàng";
            case SHIP_DELIVERING: return "Đang giao hàng";
            case SHIP_RECEIVED: return "Đã nhận hàng";
            default: return shipStep;
        }
    }

    @Exclude public String getStatusLabel() {
        if (status == null) return "";
        switch (status) {
            case STATUS_OFFERED: return "Đang chờ phản hồi";
            case STATUS_ACCEPTED: return "Đã chấp nhận";
            case STATUS_REJECTED: return "Đã từ chối";
            case STATUS_CANCELLED: return "Đã hủy";
            case STATUS_SCHEDULED: return "Đã hẹn gặp";
            case STATUS_SHIPPING: return "Giao hàng";
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
            case STATUS_SHIPPING: return 0xFF0F766E;
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
            case STATUS_SHIPPING: return 0xFFE2F4EB;
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
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String v) { deliveryMethod = v; }
    public MeetingPlace getMeetingPlace() { return meetingPlace; }
    public void setMeetingPlace(MeetingPlace v) { meetingPlace = v; }
    public Date getMeetingTime() { return meetingTime; }
    public void setMeetingTime(Date v) { meetingTime = v; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress v) { shippingAddress = v; }
    public String getShipStep() { return shipStep; }
    public void setShipStep(String v) { shipStep = v; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String v) { carrier = v; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String v) { trackingCode = v; }
    public Date getShipRequestedAt() { return shipRequestedAt; }
    public void setShipRequestedAt(Date v) { shipRequestedAt = v; }
    public Date getShippedAt() { return shippedAt; }
    public void setShippedAt(Date v) { shippedAt = v; }
    public Date getDeliveringAt() { return deliveringAt; }
    public void setDeliveringAt(Date v) { deliveringAt = v; }
    public Date getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Date v) { receivedAt = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { createdAt = v; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date v) { updatedAt = v; }
}