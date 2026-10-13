package com.anurag.campusfix.dto.analytics;

public class RoomCountItem {

    private String building;
    private String floor;
    private String room;
    private long count;

    public RoomCountItem() {
    }

    public RoomCountItem(String building, String floor, String room, long count) {
        this.building = building;
        this.floor = floor;
        this.room = room;
        this.count = count;
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

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
