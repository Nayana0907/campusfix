package com.anurag.campusfix.dto.analytics;

import java.util.ArrayList;
import java.util.List;

public class CategoryPriorityAnalyticsResponse {

    private List<CategoryCountItem> categoryCounts = new ArrayList<>();
    private List<PriorityCountItem> priorityCounts = new ArrayList<>();
    private List<CategoryCountItem> mostFrequentCategories = new ArrayList<>();
    private AnalyticsDateRange dateRange;

    public CategoryPriorityAnalyticsResponse() {
    }

    public List<CategoryCountItem> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(List<CategoryCountItem> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public List<PriorityCountItem> getPriorityCounts() {
        return priorityCounts;
    }

    public void setPriorityCounts(List<PriorityCountItem> priorityCounts) {
        this.priorityCounts = priorityCounts;
    }

    public List<CategoryCountItem> getMostFrequentCategories() {
        return mostFrequentCategories;
    }

    public void setMostFrequentCategories(List<CategoryCountItem> mostFrequentCategories) {
        this.mostFrequentCategories = mostFrequentCategories;
    }

    public AnalyticsDateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(AnalyticsDateRange dateRange) {
        this.dateRange = dateRange;
    }
}
