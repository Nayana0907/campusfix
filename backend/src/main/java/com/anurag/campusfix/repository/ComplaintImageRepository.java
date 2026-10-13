package com.anurag.campusfix.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.anurag.campusfix.entity.ComplaintImageEntity;

@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImageEntity, Long> {

    List<ComplaintImageEntity> findByComplaintId(Long complaintId);

    Optional<ComplaintImageEntity> findByStorageKey(String storageKey);
}
