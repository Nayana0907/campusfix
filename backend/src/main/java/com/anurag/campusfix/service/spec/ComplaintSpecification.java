package com.anurag.campusfix.service.spec;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;

import jakarta.persistence.criteria.Predicate;

public class ComplaintSpecification {

    public static Specification<ComplaintEntity> filterBy(
            String search,
            ComplaintCategory category,
            ComplaintPriority priority,
            ComplaintStatus status,
            String building,
            String floor,
            String room,
            Long reporterId,
            Long assignedStaffId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descPred = cb.like(cb.lower(root.get("description")), pattern);
                Predicate refPred = cb.like(cb.lower(root.get("referenceNumber")), pattern);
                Predicate bldPred = cb.like(cb.lower(root.get("building")), pattern);
                Predicate roomPred = cb.like(cb.lower(root.get("room")), pattern);
                predicates.add(cb.or(titlePred, descPred, refPred, bldPred, roomPred));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (building != null && !building.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("building")), building.trim().toLowerCase()));
            }

            if (floor != null && !floor.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("floor")), floor.trim().toLowerCase()));
            }

            if (room != null && !room.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("room")), room.trim().toLowerCase()));
            }

            if (reporterId != null) {
                predicates.add(cb.equal(root.get("reporter").get("id"), reporterId));
            }

            if (assignedStaffId != null) {
                predicates.add(cb.equal(root.get("assignedStaff").get("id"), assignedStaffId));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
