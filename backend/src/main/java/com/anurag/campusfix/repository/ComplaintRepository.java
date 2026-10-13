package com.anurag.campusfix.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.anurag.campusfix.entity.ComplaintEntity;

@Repository
public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long>, JpaSpecificationExecutor<ComplaintEntity> {

    Optional<ComplaintEntity> findByReferenceNumber(String referenceNumber);

    boolean existsByReferenceNumber(String referenceNumber);

    Page<ComplaintEntity> findByReporterId(Long reporterId, Pageable pageable);

    Page<ComplaintEntity> findByAssignedStaffId(Long assignedStaffId, Pageable pageable);
}
