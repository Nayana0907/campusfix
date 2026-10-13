package com.anurag.campusfix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ComplaintResolutionRequest {

    @NotBlank(message = "Resolution notes are required")
    @Size(min = 5, max = 2000, message = "Resolution notes must be between 5 and 2000 characters")
    private String resolutionNotes;

    public ComplaintResolutionRequest() {
    }

    public ComplaintResolutionRequest(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
