package com.example.efleamarket.models;

/** Thông tin công khai của người bán (đọc từ collection users của B). */
public class Seller {
    private final String uid;
    private final String fullName;
    private final String avatarUrl;
    private final double ratingAvg;
    private final int ratingCount;

    public Seller(String uid, String fullName, String avatarUrl, double ratingAvg, int ratingCount) {
        this.uid = uid;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.ratingAvg = ratingAvg;
        this.ratingCount = ratingCount;
    }

    public String getUid() { return uid; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public double getRatingAvg() { return ratingAvg; }
    public int getRatingCount() { return ratingCount; }
}