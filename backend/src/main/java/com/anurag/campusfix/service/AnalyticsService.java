package com.anurag.campusfix.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anurag.campusfix.dto.analytics.AnalyticsDateRange;
import com.anurag.campusfix.dto.analytics.BuildingCountItem;
import com.anurag.campusfix.dto.analytics.CategoryCountItem;
import com.anurag.campusfix.dto.analytics.CategoryPriorityAnalyticsResponse;
import com.anurag.campusfix.dto.analytics.FloorCountItem;
import com.anurag.campusfix.dto.analytics.LocationAnalyticsResponse;
import com.anurag.campusfix.dto.analytics.MonthlyTrendDataPoint;
import com.anurag.campusfix.dto.analytics.MonthlyTrendsResponse;
import com.anurag.campusfix.dto.analytics.PriorityCountItem;
import com.anurag.campusfix.dto.analytics.RecurringLocationItem;
import com.anurag.campusfix.dto.analytics.ResolutionPerformanceResponse;
import com.anurag.campusfix.dto.analytics.RoomCountItem;
import com.anurag.campusfix.dto.analytics.StaffPerformanceItem;
import com.anurag.campusfix.dto.analytics.StaffPerformanceResponse;
import com.anurag.campusfix.dto.analytics.SummaryAnalyticsResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.repository.ComplaintAnalyticsRepository;
import com.anurag.campusfix.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final ComplaintAnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;

    @Value("${app.analytics.reporting-timezone:UTC}")
    private String reportingTimezone;

    @Value("${app.analytics.max-range-days:366}")
    private long maxRangeDays;

    @Value("${app.analytics.sla.low-hours:72}")
    private long slaLowHours;

    @Value("${app.analytics.sla.medium-hours:48}")
    private long slaMediumHours;

    @Value("${app.analytics.sla.high-hours:24}")
    private long slaHighHours;

    @Value("${app.analytics.sla.urgent-hours:12}")
    private long slaUrgentHours;

    @Value("${app.verification.timeout-hours:72}")
    private long verificationTimeoutHours;

    @Value("${app.analytics.recurring-issue-threshold:2}")
    private int recurringIssueThreshold;

    public AnalyticsService(
            ComplaintAnalyticsRepository analyticsRepository,
            UserRepository userRepository) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
    }

    /**
     * 1. Summary Statistics: current operational status breakdown, verification status, and SLA overdue metrics.
     */
    public SummaryAnalyticsResponse getSummary(LocalDate from, LocalDate to) {
        DateRangeHolder bounds = resolveDateRange(from, to, false);
        LocalDateTime now = LocalDateTime.now(getZoneId());

        LocalDateTime urgentThreshold = now.minusHours(slaUrgentHours);
        LocalDateTime highThreshold = now.minusHours(slaHighHours);
        LocalDateTime mediumThreshold = now.minusHours(slaMediumHours);
        LocalDateTime lowThreshold = now.minusHours(slaLowHours);
        LocalDateTime verificationTimeoutThreshold = now.minusHours(verificationTimeoutHours);

        List<Object[]> rawCountsList = analyticsRepository.getSummaryCounts(bounds.fromDateTime(), bounds.toDateTime());
        SummaryAnalyticsResponse response = new SummaryAnalyticsResponse();
        response.setGeneratedAt(now);
        response.setTimezone(reportingTimezone);
        if (from != null || to != null) {
            response.setDateRange(new AnalyticsDateRange(from, to));
        }

        if (rawCountsList != null && !rawCountsList.isEmpty()) {
            Object[] row = rawCountsList.get(0);
            response.setTotalComplaints(toLong(row[0]));
            response.setNewlySubmitted(toLong(row[1]));
            response.setAssigned(toLong(row[2]));
            response.setAccepted(toLong(row[3]));
            response.setInProgress(toLong(row[4]));
            response.setResolved(toLong(row[5]));
            response.setAwaitingVerification(toLong(row[6]));
            response.setClosed(toLong(row[7]));
            response.setReopened(toLong(row[8]));
        }

        long overdueResolution = analyticsRepository.countOverdueResolution(
                bounds.fromDateTime(), bounds.toDateTime(),
                urgentThreshold, highThreshold, mediumThreshold, lowThreshold);

        long overdueVerification = analyticsRepository.countOverdueVerification(
                bounds.fromDateTime(), bounds.toDateTime(), verificationTimeoutThreshold);

        response.setOverdueResolution(overdueResolution);
        response.setOverdueVerification(overdueVerification);
        response.setOverdue(overdueResolution + overdueVerification);

        return response;
    }

    /**
     * 2. Monthly Trends: submission, resolution, and closure event series over chronological timeline.
     */
    public MonthlyTrendsResponse getMonthlyTrends(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now(getZoneId());
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusMonths(11).withDayOfMonth(1);

        DateRangeHolder bounds = resolveDateRange(effectiveFrom, effectiveTo, true);

        // Fetch aggregation tuples
        List<Object[]> subList = analyticsRepository.getMonthlySubmissions(bounds.fromDateTime(), bounds.toDateTime());
        List<Object[]> resList = analyticsRepository.getMonthlyResolutions(bounds.fromDateTime(), bounds.toDateTime());
        List<Object[]> clsList = analyticsRepository.getMonthlyClosures(bounds.fromDateTime(), bounds.toDateTime());

        Map<String, Long> subMap = toYearMonthMap(subList);
        Map<String, Long> resMap = toYearMonthMap(resList);
        Map<String, Long> clsMap = toYearMonthMap(clsList);

        YearMonth startYm = YearMonth.from(effectiveFrom);
        YearMonth endYm = YearMonth.from(effectiveTo);

        List<MonthlyTrendDataPoint> points = new ArrayList<>();
        YearMonth current = startYm;
        while (!current.isAfter(endYm)) {
            String monthKey = current.toString(); // "YYYY-MM"
            points.add(new MonthlyTrendDataPoint(
                    monthKey,
                    subMap.getOrDefault(monthKey, 0L),
                    resMap.getOrDefault(monthKey, 0L),
                    clsMap.getOrDefault(monthKey, 0L)
            ));
            current = current.plusMonths(1);
        }

        return new MonthlyTrendsResponse(reportingTimezone, "MONTH", effectiveFrom, effectiveTo, points);
    }

    /**
     * 3. Category & Priority Distributions: stable enum counts and rankings with deterministic tie-breaking.
     */
    public CategoryPriorityAnalyticsResponse getCategoriesAndPriorities(LocalDate from, LocalDate to) {
        DateRangeHolder bounds = resolveDateRange(from, to, false);

        List<Object[]> catRaw = analyticsRepository.getCategoryCounts(bounds.fromDateTime(), bounds.toDateTime());
        List<Object[]> priRaw = analyticsRepository.getPriorityCounts(bounds.fromDateTime(), bounds.toDateTime());

        Map<ComplaintCategory, Long> categoryMap = new HashMap<>();
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            categoryMap.put(cat, 0L);
        }
        if (catRaw != null) {
            for (Object[] r : catRaw) {
                if (r[0] instanceof ComplaintCategory cat) {
                    categoryMap.put(cat, toLong(r[1]));
                }
            }
        }

        Map<ComplaintPriority, Long> priorityMap = new HashMap<>();
        for (ComplaintPriority pri : ComplaintPriority.values()) {
            priorityMap.put(pri, 0L);
        }
        if (priRaw != null) {
            for (Object[] r : priRaw) {
                if (r[0] instanceof ComplaintPriority pri) {
                    priorityMap.put(pri, toLong(r[1]));
                }
            }
        }

        List<CategoryCountItem> catItems = new ArrayList<>();
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            catItems.add(new CategoryCountItem(cat, categoryMap.get(cat)));
        }

        List<PriorityCountItem> priItems = new ArrayList<>();
        for (ComplaintPriority pri : ComplaintPriority.values()) {
            priItems.add(new PriorityCountItem(pri, priorityMap.get(pri)));
        }

        List<CategoryCountItem> rankedCategories = new ArrayList<>(catItems);
        rankedCategories.sort((a, b) -> {
            int cmp = Long.compare(b.getCount(), a.getCount());
            if (cmp != 0) {
                return cmp;
            }
            return a.getCategory().name().compareTo(b.getCategory().name());
        });

        CategoryPriorityAnalyticsResponse response = new CategoryPriorityAnalyticsResponse();
        response.setCategoryCounts(catItems);
        response.setPriorityCounts(priItems);
        response.setMostFrequentCategories(rankedCategories);
        if (from != null || to != null) {
            response.setDateRange(new AnalyticsDateRange(from, to));
        }

        return response;
    }

    /**
     * 4. Location Analysis: building, floor, room breakdowns and recurring defect spot detection.
     */
    public LocationAnalyticsResponse getLocationAnalytics(
            LocalDate from, LocalDate to,
            String buildingFilter,
            ComplaintCategory categoryFilter,
            ComplaintPriority priorityFilter,
            int page, int size, String sort) {

        DateRangeHolder bounds = resolveDateRange(from, to, false);

        List<Object[]> buildingRaw = analyticsRepository.getBuildingCounts(
                bounds.fromDateTime(), bounds.toDateTime(), buildingFilter, categoryFilter, priorityFilter);
        List<Object[]> floorRaw = analyticsRepository.getFloorCounts(
                bounds.fromDateTime(), bounds.toDateTime(), buildingFilter, categoryFilter, priorityFilter);
        List<Object[]> roomRaw = analyticsRepository.getRoomCounts(
                bounds.fromDateTime(), bounds.toDateTime(), buildingFilter, categoryFilter, priorityFilter);

        List<BuildingCountItem> buildingItems = new ArrayList<>();
        long totalComplaints = 0;
        if (buildingRaw != null) {
            for (Object[] r : buildingRaw) {
                String b = r[0] != null ? r[0].toString() : "Unknown";
                long c = toLong(r[1]);
                buildingItems.add(new BuildingCountItem(b, c));
                totalComplaints += c;
            }
        }

        List<FloorCountItem> floorItems = new ArrayList<>();
        if (floorRaw != null) {
            for (Object[] r : floorRaw) {
                String b = r[0] != null ? r[0].toString() : "Unknown";
                String f = r[1] != null ? r[1].toString() : "Unknown";
                floorItems.add(new FloorCountItem(b, f, toLong(r[2])));
            }
        }

        List<RoomCountItem> roomItems = new ArrayList<>();
        List<RecurringLocationItem> recurringItems = new ArrayList<>();
        if (roomRaw != null) {
            for (Object[] r : roomRaw) {
                String b = r[0] != null ? r[0].toString() : "Unknown";
                String f = r[1] != null ? r[1].toString() : "Unknown";
                String rm = r[2] != null ? r[2].toString() : "Unknown";
                long count = toLong(r[3]);

                roomItems.add(new RoomCountItem(b, f, rm, count));
                if (count >= recurringIssueThreshold) {
                    recurringItems.add(new RecurringLocationItem(b, f, rm, count));
                }
            }
        }

        // Apply pagination to room items if requested
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int start = Math.min(safePage * safeSize, roomItems.size());
        int end = Math.min(start + safeSize, roomItems.size());
        List<RoomCountItem> pagedRooms = roomItems.subList(start, end);

        LocationAnalyticsResponse response = new LocationAnalyticsResponse();
        response.setBuildingCounts(buildingItems);
        response.setFloorCounts(floorItems);
        response.setRoomCounts(pagedRooms);
        response.setRecurringLocations(recurringItems);
        response.setTotalDistinctLocations(roomItems.size());
        response.setTotalComplaintsInScope(totalComplaints);
        if (from != null || to != null) {
            response.setDateRange(new AnalyticsDateRange(from, to));
        }

        return response;
    }

    /**
     * 5. Resolution Performance: exact submission-to-resolution & resolution-to-verification durations,
     * median calculations, verification closure rates, and SLA compliance.
     */
    public ResolutionPerformanceResponse getResolutionPerformance(LocalDate from, LocalDate to) {
        DateRangeHolder bounds = resolveDateRange(from, to, false);

        // Fetch timestamps for submission-to-resolution duration
        List<Object[]> subToResList = analyticsRepository.getSubmissionToResolutionTimestamps(
                bounds.fromDateTime(), bounds.toDateTime());

        List<Double> subToResHours = new ArrayList<>();
        long excludedCount = 0;
        if (subToResList != null) {
            for (Object[] r : subToResList) {
                if (r[1] instanceof LocalDateTime createdAt && r[2] instanceof LocalDateTime resolvedAt) {
                    double hours = Duration.between(createdAt, resolvedAt).toSeconds() / 3600.0;
                    if (hours >= 0) {
                        subToResHours.add(hours);
                    } else {
                        excludedCount++;
                    }
                } else {
                    excludedCount++;
                }
            }
        }

        // Fetch timestamps for resolution-to-verification duration
        List<Object[]> resToVerList = analyticsRepository.getResolutionToVerificationTimestamps(
                bounds.fromDateTime(), bounds.toDateTime());

        List<Double> resToVerHours = new ArrayList<>();
        if (resToVerList != null) {
            for (Object[] r : resToVerList) {
                if (r[0] instanceof LocalDateTime resolvedAt && r[1] instanceof LocalDateTime verifiedAt) {
                    double hours = Duration.between(resolvedAt, verifiedAt).toSeconds() / 3600.0;
                    if (hours >= 0) {
                        resToVerHours.add(hours);
                    }
                }
            }
        }

        // Fetch performance status breakdown in scope
        List<Object[]> statusCounts = analyticsRepository.getPerformanceStatusCounts(
                bounds.fromDateTime(), bounds.toDateTime());

        long totalResolvedInScope = 0;
        long totalClosedInScope = 0;
        long totalReopenedInScope = 0;
        if (statusCounts != null && !statusCounts.isEmpty()) {
            Object[] sc = statusCounts.get(0);
            totalResolvedInScope = toLong(sc[0]);
            totalClosedInScope = toLong(sc[1]);
            totalReopenedInScope = toLong(sc[2]);
        }

        LocalDateTime now = LocalDateTime.now(getZoneId());
        long overdueCount = analyticsRepository.countOverdueResolution(
                bounds.fromDateTime(), bounds.toDateTime(),
                now.minusHours(slaUrgentHours), now.minusHours(slaHighHours),
                now.minusHours(slaMediumHours), now.minusHours(slaLowHours));

        ResolutionPerformanceResponse response = new ResolutionPerformanceResponse();
        response.setAverageSubmissionToResolutionHours(calculateAverage(subToResHours));
        response.setMedianSubmissionToResolutionHours(calculateMedian(subToResHours));
        response.setAverageResolutionToVerificationHours(calculateAverage(resToVerHours));
        response.setMedianResolutionToVerificationHours(calculateMedian(resToVerHours));

        response.setVerifiedAndClosedRate(calculateRate(totalClosedInScope, totalResolvedInScope));
        response.setReopenedRate(calculateRate(totalReopenedInScope, totalResolvedInScope));

        List<Object[]> totalSummary = analyticsRepository.getSummaryCounts(bounds.fromDateTime(), bounds.toDateTime());
        long totalComplaintsInScope = (totalSummary != null && !totalSummary.isEmpty()) ? toLong(totalSummary.get(0)[0]) : 0;
        response.setOverdueRate(calculateRate(overdueCount, totalComplaintsInScope));

        response.setTotalResolvedInScope(totalResolvedInScope);
        response.setTotalClosedInScope(totalClosedInScope);
        response.setTotalReopenedInScope(totalReopenedInScope);
        response.setTotalOverdueInScope(overdueCount);
        response.setResolutionSampleSize(subToResHours.size());
        response.setVerificationSampleSize(resToVerHours.size());
        response.setExcludedMissingTimestampsCount(excludedCount);
        response.setTimezone(reportingTimezone);
        if (from != null || to != null) {
            response.setDateRange(new AnalyticsDateRange(from, to));
        }

        return response;
    }

    /**
     * 6. Staff Performance: maintenance staff workload, resolution volume, speed, and feedback stats.
     */
    public StaffPerformanceResponse getStaffPerformance(
            LocalDate from, LocalDate to, Long staffId,
            int page, int size, String sort) {

        DateRangeHolder bounds = resolveDateRange(from, to, false);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<UserEntity> staffPage;
        if (staffId != null) {
            UserEntity specific = userRepository.findById(staffId).orElse(null);
            if (specific != null && specific.getRole() == Role.MAINTENANCE_STAFF) {
                staffPage = new PageImpl<>(List.of(specific), pageable, 1);
            } else {
                staffPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        } else {
            staffPage = userRepository.findByRole(Role.MAINTENANCE_STAFF, pageable);
        }

        // Grouped queries across all or targeted staff
        List<Object[]> statusCounts = analyticsRepository.getStaffStatusCounts(
                staffId, bounds.fromDateTime(), bounds.toDateTime());

        LocalDateTime now = LocalDateTime.now(getZoneId());
        List<Object[]> overdueCounts = analyticsRepository.getStaffOverdueCounts(
                staffId, bounds.fromDateTime(), bounds.toDateTime(),
                now.minusHours(slaUrgentHours), now.minusHours(slaHighHours),
                now.minusHours(slaMediumHours), now.minusHours(slaLowHours));

        List<Object[]> durationList = analyticsRepository.getStaffResolutionTimestamps(
                staffId, bounds.fromDateTime(), bounds.toDateTime());

        // Process status map: staffId -> status -> count
        Map<Long, Map<ComplaintStatus, Long>> staffStatusMap = new HashMap<>();
        Map<Long, Long> staffTotalAssigned = new HashMap<>();
        if (statusCounts != null) {
            for (Object[] r : statusCounts) {
                Long sId = (Long) r[0];
                ComplaintStatus st = (ComplaintStatus) r[1];
                long cnt = toLong(r[2]);
                staffStatusMap.computeIfAbsent(sId, k -> new HashMap<>()).put(st, cnt);
                staffTotalAssigned.put(sId, staffTotalAssigned.getOrDefault(sId, 0L) + cnt);
            }
        }

        // Process overdue map: staffId -> overdueCount
        Map<Long, Long> staffOverdueMap = new HashMap<>();
        if (overdueCounts != null) {
            for (Object[] r : overdueCounts) {
                Long sId = (Long) r[0];
                staffOverdueMap.put(sId, toLong(r[1]));
            }
        }

        // Process durations map: staffId -> list of hours
        Map<Long, List<Double>> staffDurationsMap = new HashMap<>();
        if (durationList != null) {
            for (Object[] r : durationList) {
                Long sId = (Long) r[0];
                if (r[1] instanceof LocalDateTime cAt && r[2] instanceof LocalDateTime rAt) {
                    double hrs = Duration.between(cAt, rAt).toSeconds() / 3600.0;
                    if (hrs >= 0) {
                        staffDurationsMap.computeIfAbsent(sId, k -> new ArrayList<>()).add(hrs);
                    }
                }
            }
        }

        List<StaffPerformanceItem> items = new ArrayList<>();
        for (UserEntity staff : staffPage.getContent()) {
            StaffPerformanceItem item = new StaffPerformanceItem();
            item.setStaffId(staff.getId());
            item.setStaffName(staff.getName());
            item.setStaffEmail(staff.getEmail());

            Map<ComplaintStatus, Long> stMap = staffStatusMap.getOrDefault(staff.getId(), Collections.emptyMap());
            long assignedTotal = staffTotalAssigned.getOrDefault(staff.getId(), 0L);
            long inProgress = stMap.getOrDefault(ComplaintStatus.IN_PROGRESS, 0L)
                    + stMap.getOrDefault(ComplaintStatus.ACCEPTED, 0L);
            long resolved = stMap.getOrDefault(ComplaintStatus.RESOLVED, 0L);
            long closed = stMap.getOrDefault(ComplaintStatus.CLOSED, 0L);
            long reopened = stMap.getOrDefault(ComplaintStatus.REOPENED, 0L);
            long overdue = staffOverdueMap.getOrDefault(staff.getId(), 0L);

            List<Double> durations = staffDurationsMap.getOrDefault(staff.getId(), Collections.emptyList());

            item.setAssignedCount(assignedTotal);
            item.setInProgressCount(inProgress);
            item.setResolvedCount(resolved);
            item.setClosedCount(closed);
            item.setReopenedCount(reopened);
            item.setOverdueCount(overdue);
            item.setAverageResolutionTimeHours(calculateAverage(durations));
            item.setResolutionSampleSize(durations.size());

            // Feedback fields: ratings module is unconfigured, return documented unavailable status
            item.setFeedbackCount(null);
            item.setAverageRating(null);
            item.setRatingSampleSize(0);
            item.setFeedbackStatus("UNAVAILABLE");

            items.add(item);
        }

        // Optional sorting
        if ("resolvedCount".equalsIgnoreCase(sort)) {
            items.sort(Comparator.comparingLong(StaffPerformanceItem::getResolvedCount).reversed());
        } else if ("assignedCount".equalsIgnoreCase(sort)) {
            items.sort(Comparator.comparingLong(StaffPerformanceItem::getAssignedCount).reversed());
        } else if ("overdueCount".equalsIgnoreCase(sort)) {
            items.sort(Comparator.comparingLong(StaffPerformanceItem::getOverdueCount).reversed());
        }

        StaffPerformanceResponse response = new StaffPerformanceResponse();
        response.setItems(items);
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalElements(staffPage.getTotalElements());
        response.setTotalPages(staffPage.getTotalPages());
        if (from != null || to != null) {
            response.setDateRange(new AnalyticsDateRange(from, to));
        }

        return response;
    }

    // ------------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------------

    private DateRangeHolder resolveDateRange(LocalDate from, LocalDate to, boolean mandatory) {
        if (from == null && to == null) {
            return new DateRangeHolder(null, null);
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date ('from') must not be after end date ('to')");
        }

        LocalDate effectiveFrom = from;
        LocalDate effectiveTo = to;

        if (effectiveFrom != null && effectiveTo != null) {
            long days = ChronoUnit.DAYS.between(effectiveFrom, effectiveTo);
            if (days > maxRangeDays) {
                throw new IllegalArgumentException(String.format(
                        "Requested date range (%d days) exceeds maximum allowed span of %d days", days, maxRangeDays));
            }
        }

        LocalDateTime fromDt = effectiveFrom != null ? effectiveFrom.atStartOfDay() : null;
        LocalDateTime toDt = effectiveTo != null ? effectiveTo.plusDays(1).atStartOfDay() : null;

        return new DateRangeHolder(fromDt, toDt);
    }

    private ZoneId getZoneId() {
        try {
            return ZoneId.of(reportingTimezone);
        } catch (Exception e) {
            log.warn("Invalid timezone {}, falling back to UTC", reportingTimezone);
            return ZoneId.of("UTC");
        }
    }

    private Map<String, Long> toYearMonthMap(List<Object[]> rawList) {
        Map<String, Long> map = new HashMap<>();
        if (rawList != null) {
            for (Object[] r : rawList) {
                if (r[0] != null && r[1] != null) {
                    int yr = ((Number) r[0]).intValue();
                    int mo = ((Number) r[1]).intValue();
                    String key = String.format("%04d-%02d", yr, mo);
                    map.put(key, toLong(r[2]));
                }
            }
        }
        return map;
    }

    private long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number num) {
            return num.longValue();
        }
        return 0L;
    }

    private Double calculateAverage(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        double sum = 0.0;
        for (Double v : values) {
            sum += v;
        }
        return Math.round((sum / values.size()) * 100.0) / 100.0;
    }

    private Double calculateMedian(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 1) {
            return Math.round(sorted.get(size / 2) * 100.0) / 100.0;
        } else {
            double median = (sorted.get((size / 2) - 1) + sorted.get(size / 2)) / 2.0;
            return Math.round(median * 100.0) / 100.0;
        }
    }

    private Double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        double rate = ((double) numerator / denominator) * 100.0;
        return Math.round(rate * 100.0) / 100.0;
    }

    private record DateRangeHolder(LocalDateTime fromDateTime, LocalDateTime toDateTime) {}
}
