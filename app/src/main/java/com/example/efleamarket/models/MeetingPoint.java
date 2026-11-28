package com.example.efleamarket.models;

import com.google.firebase.firestore.DocumentId;

public class MeetingPoint {
    @DocumentId private String id;
    private String name, address, district, note;
    private double lat, lng;
    private boolean active;

    public MeetingPoint() {}

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }
    public String getDistrict() { return district; }
    public void setDistrict(String v) { district = v; }
    public String getNote() { return note; }
    public void setNote(String v) { note = v; }
    public double getLat() { return lat; }
    public void setLat(double v) { lat = v; }
    public double getLng() { return lng; }
    public void setLng(double v) { lng = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
}