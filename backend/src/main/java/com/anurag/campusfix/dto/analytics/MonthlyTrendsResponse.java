package com.anurag.campusfix.dto.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MonthlyTrendsResponse {

    private String timezone;
    private String interval = "MONTH";
    private LocalDate from;
    private LocalDate to;
    private List<MonthlyTrendDataPoint> data = new ArrayList<>();

    public MonthlyTrendsResponse() {
    }

    public MonthlyTrendsResponse(String timezone, String interval, LocalDate from, LocalDate to, List<MonthlyTrendDataPoint> data) {
        this.timezone = timezone;
        this.interval = interval;
        this.from = from;
        this.to = to;
        this.data = data;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public List<MonthlyTrendDataPoint> getData() {
        return data;
    }

    public void setData(List<MonthlyTrendDataPoint> data) {
        this.data = data;
    }
}
