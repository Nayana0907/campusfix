package com.anurag.campusfix.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.anurag.campusfix.entity.ComplaintHistoryEntity;

@Repository
public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistoryEntity, Long> {

    List<ComplaintHistoryEntity> findByComplaintIdOrderByTimestampAsc(Long complaintId);
}
