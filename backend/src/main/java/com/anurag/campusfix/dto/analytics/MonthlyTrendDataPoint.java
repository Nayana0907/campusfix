package com.anurag.campusfix.dto.analytics;

public class MonthlyTrendDataPoint {

    private String month;
    private long submittedCount;
    private long resolvedCount;
    private long closedCount;

    public MonthlyTrendDataPoint() {
    }

    public MonthlyTrendDataPoint(String month, long submittedCount, long resolvedCount, long closedCount) {
        this.month = month;
        this.submittedCount = submittedCount;
        this.resolvedCount = resolvedCount;
        this.closedCount = closedCount;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public long getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(long submittedCount) {
        this.submittedCount = submittedCount;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public long getClosedCount() {
        return closedCount;
    }

    public void setClosedCount(long closedCount) {
        this.closedCount = closedCount;
    }
}
