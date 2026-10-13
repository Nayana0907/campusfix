package com.anurag.campusfix.dto.analytics;

public class RecurringLocationItem {

    private String building;
    private String floor;
    private String room;
    private long complaintCount;

    public RecurringLocationItem() {
    }

    public RecurringLocationItem(String building, String floor, String room, long complaintCount) {
        this.building = building;
        this.floor = floor;
        this.room = room;
        this.complaintCount = complaintCount;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public long getComplaintCount() {
        return complaintCount;
    }

    public void setComplaintCount(long complaintCount) {
        this.complaintCount = complaintCount;
    }
}
