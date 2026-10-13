package com.anurag.campusfix.dto.analytics;

public class BuildingCountItem {

    private String building;
    private long count;

    public BuildingCountItem() {
    }

    public BuildingCountItem(String building, long count) {
        this.building = building;
        this.count = count;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
