package com.anurag.campusfix.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintPriority;

@Repository
public interface ComplaintAnalyticsRepository extends JpaRepository<ComplaintEntity, Long> {

    @Query("""
        SELECT 
            COUNT(c),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.SUBMITTED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.ASSIGNED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.ACCEPTED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.IN_PROGRESS THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.RESOLVED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.RESOLVED AND c.verificationStatus = com.anurag.campusfix.entity.VerificationStatus.PENDING THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.CLOSED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.REOPENED THEN 1L ELSE 0L END)
        FROM ComplaintEntity c
        WHERE (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
    """)
    List<Object[]> getSummaryCounts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT COUNT(c)
        FROM ComplaintEntity c
        WHERE c.status NOT IN (com.anurag.campusfix.entity.ComplaintStatus.RESOLVED, com.anurag.campusfix.entity.ComplaintStatus.CLOSED)
          AND (
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.URGENT AND c.createdAt < :urgentThreshold) OR
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.HIGH AND c.createdAt < :highThreshold) OR
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.MEDIUM AND c.createdAt < :mediumThreshold) OR
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.LOW AND c.createdAt < :lowThreshold)
          )
          AND (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
    """)
    long countOverdueResolution(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("urgentThreshold") LocalDateTime urgentThreshold,
            @Param("highThreshold") LocalDateTime highThreshold,
            @Param("mediumThreshold") LocalDateTime mediumThreshold,
            @Param("lowThreshold") LocalDateTime lowThreshold
    );

    @Query("""
        SELECT COUNT(c)
        FROM ComplaintEntity c
        WHERE c.status = com.anurag.campusfix.entity.ComplaintStatus.RESOLVED
          AND c.verificationStatus = com.anurag.campusfix.entity.VerificationStatus.PENDING
          AND c.resolvedAt < :verificationTimeoutThreshold
          AND (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
    """)
    long countOverdueVerification(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("verificationTimeoutThreshold") LocalDateTime verificationTimeoutThreshold
    );

    @Query("""
        SELECT YEAR(c.createdAt), MONTH(c.createdAt), COUNT(c)
        FROM ComplaintEntity c
        WHERE c.createdAt >= :from AND c.createdAt < :to
        GROUP BY YEAR(c.createdAt), MONTH(c.createdAt)
        ORDER BY YEAR(c.createdAt) ASC, MONTH(c.createdAt) ASC
    """)
    List<Object[]> getMonthlySubmissions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT YEAR(v.resolvedAt), MONTH(v.resolvedAt), COUNT(v)
        FROM ComplaintVerificationEntity v
        WHERE v.resolvedAt >= :from AND v.resolvedAt < :to
        GROUP BY YEAR(v.resolvedAt), MONTH(v.resolvedAt)
        ORDER BY YEAR(v.resolvedAt) ASC, MONTH(v.resolvedAt) ASC
    """)
    List<Object[]> getMonthlyResolutions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT YEAR(c.verifiedAt), MONTH(c.verifiedAt), COUNT(c)
        FROM ComplaintEntity c
        WHERE c.status = com.anurag.campusfix.entity.ComplaintStatus.CLOSED
          AND c.verifiedAt IS NOT NULL
          AND c.verifiedAt >= :from AND c.verifiedAt < :to
        GROUP BY YEAR(c.verifiedAt), MONTH(c.verifiedAt)
        ORDER BY YEAR(c.verifiedAt) ASC, MONTH(c.verifiedAt) ASC
    """)
    List<Object[]> getMonthlyClosures(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT c.category, COUNT(c)
        FROM ComplaintEntity c
        WHERE (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
        GROUP BY c.category
    """)
    List<Object[]> getCategoryCounts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT c.priority, COUNT(c)
        FROM ComplaintEntity c
        WHERE (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
        GROUP BY c.priority
    """)
    List<Object[]> getPriorityCounts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT c.building, COUNT(c)
        FROM ComplaintEntity c
        WHERE (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
          AND (:building IS NULL OR LOWER(c.building) = LOWER(:building))
          AND (:category IS NULL OR c.category = :category)
          AND (:priority IS NULL OR c.priority = :priority)
        GROUP BY c.building
        ORDER BY COUNT(c) DESC, c.building ASC
    """)
    List<Object[]> getBuildingCounts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("building") String building,
            @Param("category") ComplaintCategory category,
            @Param("priority") ComplaintPriority priority
    );

    @Query("""
        SELECT c.building, c.floor, COUNT(c)
        FROM ComplaintEntity c
        WHERE (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
          AND (:building IS NULL OR LOWER(c.building) = LOWER(:building))
          AND (:category IS NULL OR c.category = :category)
          AND (:priority IS NULL OR c.priority = :priority)
        GROUP BY c.building, c.floor
        ORDER BY COUNT(c) DESC, c.building ASC, c.floor ASC
    """)
    List<Object[]> getFloorCounts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("building") String building,
            @Param("category") ComplaintCategory category,
            @Param("priority") ComplaintPriority priority
    );

    @Query("""
        SELECT c.building, c.floor, c.room, COUNT(c)
        FROM ComplaintEntity c
        WHERE (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
          AND (:building IS NULL OR LOWER(c.building) = LOWER(:building))
          AND (:category IS NULL OR c.category = :category)
          AND (:priority IS NULL OR c.priority = :priority)
        GROUP BY c.building, c.floor, c.room
        ORDER BY COUNT(c) DESC, c.building ASC, c.floor ASC, c.room ASC
    """)
    List<Object[]> getRoomCounts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("building") String building,
            @Param("category") ComplaintCategory category,
            @Param("priority") ComplaintPriority priority
    );

    @Query("""
        SELECT c.id, c.createdAt, c.resolvedAt
        FROM ComplaintEntity c
        WHERE c.resolvedAt IS NOT NULL
          AND (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
    """)
    List<Object[]> getSubmissionToResolutionTimestamps(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT v.resolvedAt, v.verifiedAt
        FROM ComplaintVerificationEntity v
        WHERE v.resolvedAt IS NOT NULL
          AND v.verifiedAt IS NOT NULL
          AND (:from IS NULL OR v.resolvedAt >= :from)
          AND (:to IS NULL OR v.resolvedAt < :to)
    """)
    List<Object[]> getResolutionToVerificationTimestamps(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT 
            SUM(CASE WHEN c.status IN (com.anurag.campusfix.entity.ComplaintStatus.RESOLVED, com.anurag.campusfix.entity.ComplaintStatus.CLOSED) THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.CLOSED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.anurag.campusfix.entity.ComplaintStatus.REOPENED THEN 1L ELSE 0L END)
        FROM ComplaintEntity c
        WHERE (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
    """)
    List<Object[]> getPerformanceStatusCounts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT c.assignedStaff.id, c.status, COUNT(c)
        FROM ComplaintEntity c
        WHERE c.assignedStaff IS NOT NULL
          AND (:staffId IS NULL OR c.assignedStaff.id = :staffId)
          AND (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
        GROUP BY c.assignedStaff.id, c.status
    """)
    List<Object[]> getStaffStatusCounts(
            @Param("staffId") Long staffId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT c.assignedStaff.id, COUNT(c)
        FROM ComplaintEntity c
        WHERE c.assignedStaff IS NOT NULL
          AND (:staffId IS NULL OR c.assignedStaff.id = :staffId)
          AND c.status NOT IN (com.anurag.campusfix.entity.ComplaintStatus.RESOLVED, com.anurag.campusfix.entity.ComplaintStatus.CLOSED)
          AND (
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.URGENT AND c.createdAt < :urgentThreshold) OR
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.HIGH AND c.createdAt < :highThreshold) OR
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.MEDIUM AND c.createdAt < :mediumThreshold) OR
            (c.priority = com.anurag.campusfix.entity.ComplaintPriority.LOW AND c.createdAt < :lowThreshold)
          )
          AND (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
        GROUP BY c.assignedStaff.id
    """)
    List<Object[]> getStaffOverdueCounts(
            @Param("staffId") Long staffId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("urgentThreshold") LocalDateTime urgentThreshold,
            @Param("highThreshold") LocalDateTime highThreshold,
            @Param("mediumThreshold") LocalDateTime mediumThreshold,
            @Param("lowThreshold") LocalDateTime lowThreshold
    );

    @Query("""
        SELECT c.assignedStaff.id, c.createdAt, c.resolvedAt
        FROM ComplaintEntity c
        WHERE c.assignedStaff IS NOT NULL
          AND c.resolvedAt IS NOT NULL
          AND (:staffId IS NULL OR c.assignedStaff.id = :staffId)
          AND (:from IS NULL OR c.createdAt >= :from)
          AND (:to IS NULL OR c.createdAt < :to)
    """)
    List<Object[]> getStaffResolutionTimestamps(
            @Param("staffId") Long staffId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
