package com.example.efleamarket.models;

import com.google.firebase.firestore.Exclude;

import java.util.HashMap;
import java.util.Map;

public class ShippingAddress {
    private String name, phone, address;
    private double lat, lng;

    public ShippingAddress() {}

    public ShippingAddress(String name, String phone, String address, double lat, double lng) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
    }

    @Exclude public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("phone", phone);
        m.put("address", address);
        m.put("lat", lat);
        m.put("lng", lng);
        return m;
    }

    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }
    public double getLat() { return lat; }
    public void setLat(double v) { lat = v; }
    public double getLng() { return lng; }
    public void setLng(double v) { lng = v; }
}