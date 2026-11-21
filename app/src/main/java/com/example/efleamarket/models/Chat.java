package com.example.efleamarket.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Map;
public class Chat {
    @DocumentId private String id;
    private String productId, productTitle, productImage;
    private double productPrice;
    private String buyerId, buyerName, buyerAvatar;
    private String sellerId, sellerName, sellerAvatar;
    private List<String> participants;
    private String lastMessage, lastSenderId;
    private Map<String, Long> unread;
    private Map<String, Date> deletedAt;
    @ServerTimestamp private Date updatedAt;
    @ServerTimestamp private Date createdAt;

    public Chat() {}

    public static String buildId(String productId, String buyerId) {
        return productId + "_" + buyerId;
    }

    @Exclude public boolean isBuyer(String myUid)        { return myUid != null && myUid.equals(buyerId); }
    @Exclude public String getOtherId(String myUid)      { return isBuyer(myUid) ? sellerId : buyerId; }
    @Exclude public String getOtherName(String myUid)    { return isBuyer(myUid) ? sellerName : buyerName; }
    @Exclude public String getOtherAvatar(String myUid)  { return isBuyer(myUid) ? sellerAvatar : buyerAvatar; }
    @Exclude public String getMyRole(String myUid)       { return isBuyer(myUid) ? "buyer" : "seller"; }

    @Exclude public int getUnreadFor(String uid) {
        if (unread == null || uid == null) return 0;
        Long n = unread.get(uid);
        return n == null ? 0 : n.intValue();
    }

    @Exclude public Date getDeletedAtFor(String uid) {
        if (deletedAt == null || uid == null) return null;
        return deletedAt.get(uid);
    }

    @Exclude public boolean isHiddenFor(String uid) {
        Date d = getDeletedAtFor(uid);
        return d != null && (updatedAt == null || !updatedAt.after(d));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String v) { productTitle = v; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String v) { productImage = v; }
    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double v) { productPrice = v; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String v) { buyerId = v; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String v) { buyerName = v; }
    public String getBuyerAvatar() { return buyerAvatar; }
    public void setBuyerAvatar(String v) { buyerAvatar = v; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String v) { sellerId = v; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String v) { sellerName = v; }
    public String getSellerAvatar() { return sellerAvatar; }
    public void setSellerAvatar(String v) { sellerAvatar = v; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> v) { participants = v; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String v) { lastMessage = v; }
    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String v) { lastSenderId = v; }
    public Map<String, Long> getUnread() { return unread; }
    public void setUnread(Map<String, Long> v) { unread = v; }
    public Map<String, Date> getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Map<String, Date> v) { deletedAt = v; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date v) { updatedAt = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { createdAt = v; }
}