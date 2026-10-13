package com.anurag.campusfix.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.anurag.campusfix.dto.analytics.CategoryPriorityAnalyticsResponse;
import com.anurag.campusfix.dto.analytics.LocationAnalyticsResponse;
import com.anurag.campusfix.dto.analytics.MonthlyTrendsResponse;
import com.anurag.campusfix.dto.analytics.ResolutionPerformanceResponse;
import com.anurag.campusfix.dto.analytics.StaffPerformanceResponse;
import com.anurag.campusfix.dto.analytics.SummaryAnalyticsResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.repository.ComplaintAnalyticsRepository;
import com.anurag.campusfix.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ComplaintAnalyticsRepository analyticsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(analyticsService, "reportingTimezone", "UTC");
        ReflectionTestUtils.setField(analyticsService, "maxRangeDays", 366L);
        ReflectionTestUtils.setField(analyticsService, "slaLowHours", 72L);
        ReflectionTestUtils.setField(analyticsService, "slaMediumHours", 48L);
        ReflectionTestUtils.setField(analyticsService, "slaHighHours", 24L);
        ReflectionTestUtils.setField(analyticsService, "slaUrgentHours", 12L);
        ReflectionTestUtils.setField(analyticsService, "verificationTimeoutHours", 72L);
        ReflectionTestUtils.setField(analyticsService, "recurringIssueThreshold", 2);
    }

    @Test
    @DisplayName("getSummary: aggregates all operational statuses and overdue metrics correctly")
    void getSummary_Success() {
        Object[] rawRow = new Object[]{
                100L, // total
                15L,  // submitted
                10L,  // assigned
                5L,   // accepted
                20L,  // inProgress
                25L,  // resolved
                10L,  // awaitingVerification
                20L,  // closed
                5L    // reopened
        };

        when(analyticsRepository.getSummaryCounts(null, null))
                .thenReturn(Collections.singletonList(rawRow));
        when(analyticsRepository.countOverdueResolution(any(), any(), any(), any(), any(), any()))
                .thenReturn(7L);
        when(analyticsRepository.countOverdueVerification(any(), any(), any()))
                .thenReturn(3L);

        SummaryAnalyticsResponse response = analyticsService.getSummary(null, null);

        assertNotNull(response);
        assertEquals(100L, response.getTotalComplaints());
        assertEquals(15L, response.getNewlySubmitted());
        assertEquals(10L, response.getAssigned());
        assertEquals(5L, response.getAccepted());
        assertEquals(20L, response.getInProgress());
        assertEquals(25L, response.getResolved());
        assertEquals(10L, response.getAwaitingVerification());
        assertEquals(20L, response.getClosed());
        assertEquals(5L, response.getReopened());
        assertEquals(7L, response.getOverdueResolution());
        assertEquals(3L, response.getOverdueVerification());
        assertEquals(10L, response.getOverdue());
        assertEquals("UTC", response.getTimezone());
        assertNotNull(response.getGeneratedAt());
        assertNull(response.getDateRange());
    }

    @Test
    @DisplayName("getSummary: handles empty database safely returning zeroes")
    void getSummary_EmptyDatabase() {
        when(analyticsRepository.getSummaryCounts(any(), any()))
                .thenReturn(Collections.emptyList());
        when(analyticsRepository.countOverdueResolution(any(), any(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(analyticsRepository.countOverdueVerification(any(), any(), any()))
                .thenReturn(0L);

        SummaryAnalyticsResponse response = analyticsService.getSummary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertNotNull(response);
        assertEquals(0L, response.getTotalComplaints());
        assertEquals(0L, response.getOverdue());
        assertNotNull(response.getDateRange());
        assertEquals(LocalDate.of(2026, 1, 1), response.getDateRange().getFrom());
        assertEquals(LocalDate.of(2026, 1, 31), response.getDateRange().getTo());
    }

    @Test
    @DisplayName("getMonthlyTrends: generates continuous zero-filled chronological monthly sequence")
    void getMonthlyTrends_Success() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        // Submissions: Jan=10, Mar=15
        List<Object[]> submissions = List.<Object[]>of(
                new Object[]{2026, 1, 10L},
                new Object[]{2026, 3, 15L}
        );
        // Resolutions: Feb=8, Mar=12
        List<Object[]> resolutions = List.<Object[]>of(
                new Object[]{2026, 2, 8L},
                new Object[]{2026, 3, 12L}
        );
        // Closures: Mar=9
        List<Object[]> closures = List.<Object[]>of(
                new Object[]{2026, 3, 9L}
        );

        when(analyticsRepository.getMonthlySubmissions(any(), any())).thenReturn(submissions);
        when(analyticsRepository.getMonthlyResolutions(any(), any())).thenReturn(resolutions);
        when(analyticsRepository.getMonthlyClosures(any(), any())).thenReturn(closures);

        MonthlyTrendsResponse response = analyticsService.getMonthlyTrends(from, to);

        assertNotNull(response);
        assertEquals("UTC", response.getTimezone());
        assertEquals("MONTH", response.getInterval());
        assertEquals(3, response.getData().size()); // 2026-01, 2026-02, 2026-03

        // Month 1
        assertEquals("2026-01", response.getData().get(0).getMonth());
        assertEquals(10L, response.getData().get(0).getSubmittedCount());
        assertEquals(0L, response.getData().get(0).getResolvedCount());
        assertEquals(0L, response.getData().get(0).getClosedCount());

        // Month 2 (Feb had no submissions -> zero filled!)
        assertEquals("2026-02", response.getData().get(1).getMonth());
        assertEquals(0L, response.getData().get(1).getSubmittedCount());
        assertEquals(8L, response.getData().get(1).getResolvedCount());
        assertEquals(0L, response.getData().get(1).getClosedCount());

        // Month 3
        assertEquals("2026-03", response.getData().get(2).getMonth());
        assertEquals(15L, response.getData().get(2).getSubmittedCount());
        assertEquals(12L, response.getData().get(2).getResolvedCount());
        assertEquals(9L, response.getData().get(2).getClosedCount());
    }

    @Test
    @DisplayName("getCategoriesAndPriorities: includes all enum values with deterministic ranking")
    void getCategoriesAndPriorities_Success() {
        List<Object[]> catList = List.<Object[]>of(
                new Object[]{ComplaintCategory.ELECTRICAL, 30L},
                new Object[]{ComplaintCategory.PLUMBING, 20L}
        );
        List<Object[]> priList = List.<Object[]>of(
                new Object[]{ComplaintPriority.HIGH, 25L},
                new Object[]{ComplaintPriority.LOW, 10L}
        );

        when(analyticsRepository.getCategoryCounts(any(), any())).thenReturn(catList);
        when(analyticsRepository.getPriorityCounts(any(), any())).thenReturn(priList);

        CategoryPriorityAnalyticsResponse response = analyticsService.getCategoriesAndPriorities(null, null);

        assertNotNull(response);
        // Category counts must include ALL enum values (even with 0 count)
        assertEquals(ComplaintCategory.values().length, response.getCategoryCounts().size());
        assertEquals(ComplaintPriority.values().length, response.getPriorityCounts().size());

        // Rankings sorted desc by count
        assertEquals(ComplaintCategory.ELECTRICAL, response.getMostFrequentCategories().get(0).getCategory());
        assertEquals(30L, response.getMostFrequentCategories().get(0).getCount());
        assertEquals(ComplaintCategory.PLUMBING, response.getMostFrequentCategories().get(1).getCategory());
        assertEquals(20L, response.getMostFrequentCategories().get(1).getCount());
    }

    @Test
    @DisplayName("getLocationAnalytics: aggregates building, floor, room, and recurring problem spots")
    void getLocationAnalytics_Success() {
        List<Object[]> buildings = List.<Object[]>of(
                new Object[]{"Engineering Block A", 25L},
                new Object[]{"Science Block B", 10L}
        );
        List<Object[]> floors = List.<Object[]>of(
                new Object[]{"Engineering Block A", "1st Floor", 15L},
                new Object[]{"Engineering Block A", "2nd Floor", 10L}
        );
        List<Object[]> rooms = List.<Object[]>of(
                new Object[]{"Engineering Block A", "1st Floor", "Lab 101", 5L}, // recurring (>=2)
                new Object[]{"Engineering Block A", "1st Floor", "Lab 102", 1L}  // non-recurring (<2)
        );

        when(analyticsRepository.getBuildingCounts(any(), any(), any(), any(), any())).thenReturn(buildings);
        when(analyticsRepository.getFloorCounts(any(), any(), any(), any(), any())).thenReturn(floors);
        when(analyticsRepository.getRoomCounts(any(), any(), any(), any(), any())).thenReturn(rooms);

        LocationAnalyticsResponse response = analyticsService.getLocationAnalytics(
                null, null, null, null, null, 0, 20, null);

        assertNotNull(response);
        assertEquals(35L, response.getTotalComplaintsInScope());
        assertEquals(2, response.getBuildingCounts().size());
        assertEquals(2, response.getFloorCounts().size());
        assertEquals(2, response.getRoomCounts().size());
        assertEquals(1, response.getRecurringLocations().size());
        assertEquals("Lab 101", response.getRecurringLocations().get(0).getRoom());
        assertEquals(5L, response.getRecurringLocations().get(0).getComplaintCount());
    }

    @Test
    @DisplayName("getResolutionPerformance: calculates average and median durations, completion and SLA rates accurately")
    void getResolutionPerformance_Success() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);

        // 3 durations: 2h, 4h, 6h -> average = 4.0h, median = 4.0h
        List<Object[]> subToRes = List.<Object[]>of(
                new Object[]{1L, base, base.plusHours(2)},
                new Object[]{2L, base, base.plusHours(4)},
                new Object[]{3L, base, base.plusHours(6)}
        );

        // 2 verification durations: 1h, 3h -> average = 2.0h, median = 2.0h
        List<Object[]> resToVer = List.<Object[]>of(
                new Object[]{base, base.plusHours(1)},
                new Object[]{base, base.plusHours(3)}
        );

        // Status counts: totalResolved=10, closed=8, reopened=2
        List<Object[]> statusCounts = Collections.singletonList(
                new Object[]{10L, 8L, 2L}
        );

        when(analyticsRepository.getSubmissionToResolutionTimestamps(any(), any())).thenReturn(subToRes);
        when(analyticsRepository.getResolutionToVerificationTimestamps(any(), any())).thenReturn(resToVer);
        when(analyticsRepository.getPerformanceStatusCounts(any(), any())).thenReturn(statusCounts);
        when(analyticsRepository.countOverdueResolution(any(), any(), any(), any(), any(), any())).thenReturn(2L);
        when(analyticsRepository.getSummaryCounts(any(), any())).thenReturn(Collections.singletonList(new Object[]{10L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L}));

        ResolutionPerformanceResponse response = analyticsService.getResolutionPerformance(null, null);

        assertNotNull(response);
        assertEquals(4.0, response.getAverageSubmissionToResolutionHours());
        assertEquals(4.0, response.getMedianSubmissionToResolutionHours());
        assertEquals(2.0, response.getAverageResolutionToVerificationHours());
        assertEquals(2.0, response.getMedianResolutionToVerificationHours());
        assertEquals(80.0, response.getVerifiedAndClosedRate()); // 8 / 10 * 100
        assertEquals(20.0, response.getReopenedRate());           // 2 / 10 * 100
        assertEquals(20.0, response.getOverdueRate());            // 2 / 10 * 100
        assertEquals(3L, response.getResolutionSampleSize());
        assertEquals(2L, response.getVerificationSampleSize());
        assertEquals(0L, response.getExcludedMissingTimestampsCount());
    }

    @Test
    @DisplayName("getStaffPerformance: reports workload, speed, and marks feedback as UNAVAILABLE")
    void getStaffPerformance_Success() {
        UserEntity staff1 = new UserEntity();
        staff1.setId(101L);
        staff1.setName("Charlie Plumber");
        staff1.setEmail("charlie@campus.edu");
        staff1.setRole(Role.MAINTENANCE_STAFF);

        when(userRepository.findByRole(eq(Role.MAINTENANCE_STAFF), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(staff1)));

        // Staff status: 101 has 3 IN_PROGRESS, 5 RESOLVED, 2 CLOSED
        List<Object[]> statusCounts = List.<Object[]>of(
                new Object[]{101L, ComplaintStatus.IN_PROGRESS, 3L},
                new Object[]{101L, ComplaintStatus.RESOLVED, 5L},
                new Object[]{101L, ComplaintStatus.CLOSED, 2L}
        );
        when(analyticsRepository.getStaffStatusCounts(any(), any(), any())).thenReturn(statusCounts);
        when(analyticsRepository.getStaffOverdueCounts(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{101L, 1L}));

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> durations = Collections.singletonList(
                new Object[]{101L, now.minusHours(4), now} // 4h
        );
        when(analyticsRepository.getStaffResolutionTimestamps(any(), any(), any())).thenReturn(durations);

        StaffPerformanceResponse response = analyticsService.getStaffPerformance(
                null, null, null, 0, 20, null);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());

        var item = response.getItems().get(0);
        assertEquals(101L, item.getStaffId());
        assertEquals("Charlie Plumber", item.getStaffName());
        assertEquals(10L, item.getAssignedCount());
        assertEquals(3L, item.getInProgressCount());
        assertEquals(5L, item.getResolvedCount());
        assertEquals(2L, item.getClosedCount());
        assertEquals(1L, item.getOverdueCount());
        assertEquals(4.0, item.getAverageResolutionTimeHours());
        assertEquals(1L, item.getResolutionSampleSize());

        // Feedback / rating safety invariants
        assertNull(item.getFeedbackCount());
        assertNull(item.getAverageRating());
        assertEquals(0L, item.getRatingSampleSize());
        assertEquals("UNAVAILABLE", item.getFeedbackStatus());
    }

    @Test
    @DisplayName("Validation: start date after end date throws IllegalArgumentException")
    void dateValidation_StartAfterEnd_Throws() {
        LocalDate from = LocalDate.of(2026, 5, 10);
        LocalDate to = LocalDate.of(2026, 5, 1);

        assertThrows(IllegalArgumentException.class, () ->
                analyticsService.getSummary(from, to));
    }

    @Test
    @DisplayName("Validation: date range exceeding maximum span throws IllegalArgumentException")
    void dateValidation_ExceedsMaxSpan_Throws() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 1); // 2 years > 366 days

        assertThrows(IllegalArgumentException.class, () ->
                analyticsService.getSummary(from, to));
    }
}
