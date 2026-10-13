package com.anurag.campusfix.dto.request;

import jakarta.validation.constraints.NotNull;

public class ComplaintAssignmentRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    public ComplaintAssignmentRequest() {
    }

    public ComplaintAssignmentRequest(Long staffId) {
        this.staffId = staffId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
}
