package com.anurag.campusfix.dto.analytics;

import java.util.ArrayList;
import java.util.List;

public class LocationAnalyticsResponse {

    private List<BuildingCountItem> buildingCounts = new ArrayList<>();
    private List<FloorCountItem> floorCounts = new ArrayList<>();
    private List<RoomCountItem> roomCounts = new ArrayList<>();
    private List<RecurringLocationItem> recurringLocations = new ArrayList<>();
    private long totalDistinctLocations;
    private long totalComplaintsInScope;
    private AnalyticsDateRange dateRange;

    public LocationAnalyticsResponse() {
    }

    public List<BuildingCountItem> getBuildingCounts() {
        return buildingCounts;
    }

    public void setBuildingCounts(List<BuildingCountItem> buildingCounts) {
        this.buildingCounts = buildingCounts;
    }

    public List<FloorCountItem> getFloorCounts() {
        return floorCounts;
    }

    public void setFloorCounts(List<FloorCountItem> floorCounts) {
        this.floorCounts = floorCounts;
    }

    public List<RoomCountItem> getRoomCounts() {
        return roomCounts;
    }

    public void setRoomCounts(List<RoomCountItem> roomCounts) {
        this.roomCounts = roomCounts;
    }

    public List<RecurringLocationItem> getRecurringLocations() {
        return recurringLocations;
    }

    public void setRecurringLocations(List<RecurringLocationItem> recurringLocations) {
        this.recurringLocations = recurringLocations;
    }

    public long getTotalDistinctLocations() {
        return totalDistinctLocations;
    }

    public void setTotalDistinctLocations(long totalDistinctLocations) {
        this.totalDistinctLocations = totalDistinctLocations;
    }

    public long getTotalComplaintsInScope() {
        return totalComplaintsInScope;
    }

    public void setTotalComplaintsInScope(long totalComplaintsInScope) {
        this.totalComplaintsInScope = totalComplaintsInScope;
    }

    public AnalyticsDateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(AnalyticsDateRange dateRange) {
        this.dateRange = dateRange;
    }
}
