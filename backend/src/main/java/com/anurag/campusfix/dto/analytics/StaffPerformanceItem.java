package com.anurag.campusfix.dto.analytics;

public class StaffPerformanceItem {

    private Long staffId;
    private String staffName;
    private String staffEmail;
    private long assignedCount;
    private long inProgressCount;
    private long resolvedCount;
    private long closedCount;
    private long reopenedCount;
    private long overdueCount;
    private Double averageResolutionTimeHours;
    private long resolutionSampleSize;
    private Long feedbackCount;
    private Double averageRating;
    private long ratingSampleSize;
    private String feedbackStatus = "UNAVAILABLE";

    public StaffPerformanceItem() {
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffEmail() {
        return staffEmail;
    }

    public void setStaffEmail(String staffEmail) {
        this.staffEmail = staffEmail;
    }

    public long getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(long assignedCount) {
        this.assignedCount = assignedCount;
    }

    public long getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(long inProgressCount) {
        this.inProgressCount = inProgressCount;
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

    public long getReopenedCount() {
        return reopenedCount;
    }

    public void setReopenedCount(long reopenedCount) {
        this.reopenedCount = reopenedCount;
    }

    public long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(long overdueCount) {
        this.overdueCount = overdueCount;
    }

    public Double getAverageResolutionTimeHours() {
        return averageResolutionTimeHours;
    }

    public void setAverageResolutionTimeHours(Double averageResolutionTimeHours) {
        this.averageResolutionTimeHours = averageResolutionTimeHours;
    }

    public long getResolutionSampleSize() {
        return resolutionSampleSize;
    }

    public void setResolutionSampleSize(long resolutionSampleSize) {
        this.resolutionSampleSize = resolutionSampleSize;
    }

    public Long getFeedbackCount() {
        return feedbackCount;
    }

    public void setFeedbackCount(Long feedbackCount) {
        this.feedbackCount = feedbackCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public long getRatingSampleSize() {
        return ratingSampleSize;
    }

    public void setRatingSampleSize(long ratingSampleSize) {
        this.ratingSampleSize = ratingSampleSize;
    }

    public String getFeedbackStatus() {
        return feedbackStatus;
    }

    public void setFeedbackStatus(String feedbackStatus) {
        this.feedbackStatus = feedbackStatus;
    }
}
