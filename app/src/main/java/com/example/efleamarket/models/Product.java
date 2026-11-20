package com.example.efleamarket.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Product {

    // Giá trị hợp lệ của trường status (A định nghĩa, Admin của B chỉ cập nhật)
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SELLING = "selling";
    public static final String STATUS_SOLD = "sold";
    public static final String STATUS_HIDDEN = "hidden";

    @DocumentId
    private String id;
    private String title;
    private double price;
    private String categoryId;
    private String condition;
    private String description;
    private String location;
    private List<String> images = new ArrayList<>();    // Danh sách URL ảnh, ảnh đầu = ảnh bìa
    private List<String> keywords = new ArrayList<>();  // Từ khoá (dùng cho tìm kiếm và AI)
    private String imageUrl;                            // CŨ: chỉ giữ để đọc dữ liệu mẫu cũ
    private String sellerId;
    private String sellerName;
    private String status;

    @ServerTimestamp
    private Date createdAt;

    // Bắt buộc có constructor rỗng cho Firebase
    public Product() {}

    // Giữ nguyên chữ ký cũ để DataSeeder không phải sửa
    public Product(String title, double price, String categoryId, String condition,
                   String description, String location, String imageUrl,
                   String sellerId, String sellerName, String status) {
        this.title = title;
        this.price = price;
        this.categoryId = categoryId;
        this.condition = condition;
        this.description = description;
        this.location = location;
        if (imageUrl != null && !imageUrl.isEmpty()) this.images.add(imageUrl);
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.status = status;
    }

    /** Ảnh bìa: ảnh đầu tiên trong images, nếu không có thì lấy imageUrl cũ. Không lưu lên Firestore. */
    @Exclude
    public String getCoverImage() {
        if (images != null && !images.isEmpty()) return images.get(0);
        return imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}