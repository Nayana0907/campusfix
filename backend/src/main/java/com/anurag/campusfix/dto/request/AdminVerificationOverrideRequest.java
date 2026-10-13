package com.anurag.campusfix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.anurag.campusfix.entity.VerificationStatus;

public class AdminVerificationOverrideRequest {

    @NotNull(message = "Decision is required (must be VERIFIED or REJECTED)")
    private VerificationStatus decision;

    @NotBlank(message = "Admin override reason is required")
    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;

    public AdminVerificationOverrideRequest() {
    }

    public AdminVerificationOverrideRequest(VerificationStatus decision, String reason) {
        this.decision = decision;
        this.reason = reason;
    }

    public VerificationStatus getDecision() {
        return decision;
    }

    public void setDecision(VerificationStatus decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
