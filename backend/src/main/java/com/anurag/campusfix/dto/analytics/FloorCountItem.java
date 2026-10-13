package com.anurag.campusfix.dto.analytics;

public class FloorCountItem {

    private String building;
    private String floor;
    private long count;

    public FloorCountItem() {
    }

    public FloorCountItem(String building, String floor, long count) {
        this.building = building;
        this.floor = floor;
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

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
