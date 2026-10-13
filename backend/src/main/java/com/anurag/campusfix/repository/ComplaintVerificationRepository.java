package com.anurag.campusfix.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.VerificationStatus;

@Repository
public interface ComplaintVerificationRepository extends JpaRepository<ComplaintVerificationEntity, Long> {

    List<ComplaintVerificationEntity> findByComplaintIdOrderByCycleNumberAsc(Long complaintId);

    Optional<ComplaintVerificationEntity> findTopByComplaintIdOrderByCycleNumberDesc(Long complaintId);

    Optional<ComplaintVerificationEntity> findByComplaintIdAndVerificationStatus(Long complaintId, VerificationStatus verificationStatus);

    List<ComplaintVerificationEntity> findByVerificationStatus(VerificationStatus verificationStatus);
}
