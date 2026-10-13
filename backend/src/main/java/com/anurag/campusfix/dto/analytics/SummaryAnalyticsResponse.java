package com.anurag.campusfix.dto.analytics;

import java.time.LocalDateTime;

public class SummaryAnalyticsResponse {

    private long totalComplaints;
    private long newlySubmitted;
    private long assigned;
    private long accepted;
    private long inProgress;
    private long resolved;
    private long awaitingVerification;
    private long closed;
    private long reopened;
    private long overdue;
    private long overdueResolution;
    private long overdueVerification;
    private LocalDateTime generatedAt;
    private String timezone;
    private AnalyticsDateRange dateRange;

    public SummaryAnalyticsResponse() {
    }

    public long getTotalComplaints() {
        return totalComplaints;
    }

    public void setTotalComplaints(long totalComplaints) {
        this.totalComplaints = totalComplaints;
    }

    public long getNewlySubmitted() {
        return newlySubmitted;
    }

    public void setNewlySubmitted(long newlySubmitted) {
        this.newlySubmitted = newlySubmitted;
    }

    public long getAssigned() {
        return assigned;
    }

    public void setAssigned(long assigned) {
        this.assigned = assigned;
    }

    public long getAccepted() {
        return accepted;
    }

    public void setAccepted(long accepted) {
        this.accepted = accepted;
    }

    public long getInProgress() {
        return inProgress;
    }

    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    public long getResolved() {
        return resolved;
    }

    public void setResolved(long resolved) {
        this.resolved = resolved;
    }

    public long getAwaitingVerification() {
        return awaitingVerification;
    }

    public void setAwaitingVerification(long awaitingVerification) {
        this.awaitingVerification = awaitingVerification;
    }

    public long getClosed() {
        return closed;
    }

    public void setClosed(long closed) {
        this.closed = closed;
    }

    public long getReopened() {
        return reopened;
    }

    public void setReopened(long reopened) {
        this.reopened = reopened;
    }

    public long getOverdue() {
        return overdue;
    }

    public void setOverdue(long overdue) {
        this.overdue = overdue;
    }

    public long getOverdueResolution() {
        return overdueResolution;
    }

    public void setOverdueResolution(long overdueResolution) {
        this.overdueResolution = overdueResolution;
    }

    public long getOverdueVerification() {
        return overdueVerification;
    }

    public void setOverdueVerification(long overdueVerification) {
        this.overdueVerification = overdueVerification;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public AnalyticsDateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(AnalyticsDateRange dateRange) {
        this.dateRange = dateRange;
    }
}
