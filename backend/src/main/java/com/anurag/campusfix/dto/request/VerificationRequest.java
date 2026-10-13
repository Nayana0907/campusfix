package com.anurag.campusfix.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.anurag.campusfix.entity.VerificationStatus;

public class VerificationRequest {

    @NotNull(message = "Decision is required (must be VERIFIED or REJECTED)")
    private VerificationStatus decision;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    public VerificationRequest() {
    }

    public VerificationRequest(VerificationStatus decision, String comment) {
        this.decision = decision;
        this.comment = comment;
    }

    public VerificationStatus getDecision() {
        return decision;
    }

    public void setDecision(VerificationStatus decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
