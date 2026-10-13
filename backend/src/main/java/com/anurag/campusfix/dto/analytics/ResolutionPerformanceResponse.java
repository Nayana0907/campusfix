package com.anurag.campusfix.dto.analytics;

public class ResolutionPerformanceResponse {

    private Double averageSubmissionToResolutionHours;
    private Double medianSubmissionToResolutionHours;
    private Double averageResolutionToVerificationHours;
    private Double medianResolutionToVerificationHours;
    private Double verifiedAndClosedRate;
    private Double reopenedRate;
    private Double overdueRate;
    private long totalResolvedInScope;
    private long totalClosedInScope;
    private long totalReopenedInScope;
    private long totalOverdueInScope;
    private long resolutionSampleSize;
    private long verificationSampleSize;
    private long excludedMissingTimestampsCount;
    private AnalyticsDateRange dateRange;
    private String timezone;

    public ResolutionPerformanceResponse() {
    }

    public Double getAverageSubmissionToResolutionHours() {
        return averageSubmissionToResolutionHours;
    }

    public void setAverageSubmissionToResolutionHours(Double averageSubmissionToResolutionHours) {
        this.averageSubmissionToResolutionHours = averageSubmissionToResolutionHours;
    }

    public Double getMedianSubmissionToResolutionHours() {
        return medianSubmissionToResolutionHours;
    }

    public void setMedianSubmissionToResolutionHours(Double medianSubmissionToResolutionHours) {
        this.medianSubmissionToResolutionHours = medianSubmissionToResolutionHours;
    }

    public Double getAverageResolutionToVerificationHours() {
        return averageResolutionToVerificationHours;
    }

    public void setAverageResolutionToVerificationHours(Double averageResolutionToVerificationHours) {
        this.averageResolutionToVerificationHours = averageResolutionToVerificationHours;
    }

    public Double getMedianResolutionToVerificationHours() {
        return medianResolutionToVerificationHours;
    }

    public void setMedianResolutionToVerificationHours(Double medianResolutionToVerificationHours) {
        this.medianResolutionToVerificationHours = medianResolutionToVerificationHours;
    }

    public Double getVerifiedAndClosedRate() {
        return verifiedAndClosedRate;
    }

    public void setVerifiedAndClosedRate(Double verifiedAndClosedRate) {
        this.verifiedAndClosedRate = verifiedAndClosedRate;
    }

    public Double getReopenedRate() {
        return reopenedRate;
    }

    public void setReopenedRate(Double reopenedRate) {
        this.reopenedRate = reopenedRate;
    }

    public Double getOverdueRate() {
        return overdueRate;
    }

    public void setOverdueRate(Double overdueRate) {
        this.overdueRate = overdueRate;
    }

    public long getTotalResolvedInScope() {
        return totalResolvedInScope;
    }

    public void setTotalResolvedInScope(long totalResolvedInScope) {
        this.totalResolvedInScope = totalResolvedInScope;
    }

    public long getTotalClosedInScope() {
        return totalClosedInScope;
    }

    public void setTotalClosedInScope(long totalClosedInScope) {
        this.totalClosedInScope = totalClosedInScope;
    }

    public long getTotalReopenedInScope() {
        return totalReopenedInScope;
    }

    public void setTotalReopenedInScope(long totalReopenedInScope) {
        this.totalReopenedInScope = totalReopenedInScope;
    }

    public long getTotalOverdueInScope() {
        return totalOverdueInScope;
    }

    public void setTotalOverdueInScope(long totalOverdueInScope) {
        this.totalOverdueInScope = totalOverdueInScope;
    }

    public long getResolutionSampleSize() {
        return resolutionSampleSize;
    }

    public void setResolutionSampleSize(long resolutionSampleSize) {
        this.resolutionSampleSize = resolutionSampleSize;
    }

    public long getVerificationSampleSize() {
        return verificationSampleSize;
    }

    public void setVerificationSampleSize(long verificationSampleSize) {
        this.verificationSampleSize = verificationSampleSize;
    }

    public long getExcludedMissingTimestampsCount() {
        return excludedMissingTimestampsCount;
    }

    public void setExcludedMissingTimestampsCount(long excludedMissingTimestampsCount) {
        this.excludedMissingTimestampsCount = excludedMissingTimestampsCount;
    }

    public AnalyticsDateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(AnalyticsDateRange dateRange) {
        this.dateRange = dateRange;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
