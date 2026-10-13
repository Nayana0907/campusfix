package com.anurag.campusfix.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.anurag.campusfix.entity.FeedbackEntity;

@Repository
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    Optional<FeedbackEntity> findByComplaintId(Long complaintId);

    boolean existsByComplaintId(Long complaintId);

    @Query("SELECT AVG(CAST(f.rating AS double)) FROM FeedbackEntity f WHERE f.staff.id = :staffId")
    Double getAverageRatingForStaff(@Param("staffId") Long staffId);

    @Query("SELECT COUNT(f) FROM FeedbackEntity f WHERE f.staff.id = :staffId")
    Long getFeedbackCountForStaff(@Param("staffId") Long staffId);
}
