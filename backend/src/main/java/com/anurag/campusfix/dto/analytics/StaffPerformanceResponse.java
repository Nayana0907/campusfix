package com.anurag.campusfix.dto.analytics;

import java.util.ArrayList;
import java.util.List;

public class StaffPerformanceResponse {

    private List<StaffPerformanceItem> items = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private AnalyticsDateRange dateRange;

    public StaffPerformanceResponse() {
    }

    public List<StaffPerformanceItem> getItems() {
        return items;
    }

    public void setItems(List<StaffPerformanceItem> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public AnalyticsDateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(AnalyticsDateRange dateRange) {
        this.dateRange = dateRange;
    }
}
