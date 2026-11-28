package com.example.efleamarket.models;

import com.google.firebase.firestore.Exclude;

import java.util.HashMap;
import java.util.Map;

public class MeetingPlace {
    private String name;
    private double lat, lng;
    private String pointId;

    public MeetingPlace() {}

    public MeetingPlace(String name, double lat, double lng, String pointId) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.pointId = pointId == null ? "" : pointId;
    }

    @Exclude public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name == null ? "" : name);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("pointId", pointId == null ? "" : pointId);
        return m;
    }

    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public double getLat() { return lat; }
    public void setLat(double v) { lat = v; }
    public double getLng() { return lng; }
    public void setLng(double v) { lng = v; }
    public String getPointId() { return pointId; }
    public void setPointId(String v) { pointId = v; }
}