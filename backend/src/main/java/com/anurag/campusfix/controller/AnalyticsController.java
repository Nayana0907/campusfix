package com.anurag.campusfix.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anurag.campusfix.dto.analytics.CategoryPriorityAnalyticsResponse;
import com.anurag.campusfix.dto.analytics.LocationAnalyticsResponse;
import com.anurag.campusfix.dto.analytics.MonthlyTrendsResponse;
import com.anurag.campusfix.dto.analytics.ResolutionPerformanceResponse;
import com.anurag.campusfix.dto.analytics.StaffPerformanceResponse;
import com.anurag.campusfix.dto.analytics.SummaryAnalyticsResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.service.AnalyticsService;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * 1. Overview summary cards statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<SummaryAnalyticsResponse> getSummary(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        SummaryAnalyticsResponse response = analyticsService.getSummary(from, to);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Monthly submission, resolution, and closure trends
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyTrendsResponse> getMonthlyTrends(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        MonthlyTrendsResponse response = analyticsService.getMonthlyTrends(from, to);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Category & Priority breakdowns and distribution
     */
    @GetMapping("/categories")
    public ResponseEntity<CategoryPriorityAnalyticsResponse> getCategoriesAndPriorities(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        CategoryPriorityAnalyticsResponse response = analyticsService.getCategoriesAndPriorities(from, to);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. Location hotspot, building, floor, room and recurring issue analysis
     */
    @GetMapping("/locations")
    public ResponseEntity<LocationAnalyticsResponse> getLocationAnalytics(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "building", required = false) String building,
            @RequestParam(value = "category", required = false) ComplaintCategory category,
            @RequestParam(value = "priority", required = false) ComplaintPriority priority,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort) {

        LocationAnalyticsResponse response = analyticsService.getLocationAnalytics(
                from, to, building, category, priority, page, size, sort);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. Resolution & verification cycle duration, completion rates, and SLA metrics
     */
    @GetMapping("/resolution-performance")
    public ResponseEntity<ResolutionPerformanceResponse> getResolutionPerformance(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        ResolutionPerformanceResponse response = analyticsService.getResolutionPerformance(from, to);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. Maintenance staff workload, resolution volume, speed, and feedback stats
     */
    @GetMapping("/staff-performance")
    public ResponseEntity<StaffPerformanceResponse> getStaffPerformance(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "staffId", required = false) Long staffId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort) {

        StaffPerformanceResponse response = analyticsService.getStaffPerformance(
                from, to, staffId, page, size, sort);
        return ResponseEntity.ok(response);
    }
}
