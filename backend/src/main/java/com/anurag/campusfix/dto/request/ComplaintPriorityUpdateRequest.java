package com.anurag.campusfix.dto.request;

import com.anurag.campusfix.entity.ComplaintPriority;
import jakarta.validation.constraints.NotNull;

public class ComplaintPriorityUpdateRequest {

    @NotNull(message = "Priority is required")
    private ComplaintPriority priority;

    public ComplaintPriorityUpdateRequest() {
    }

    public ComplaintPriorityUpdateRequest(ComplaintPriority priority) {
        this.priority = priority;
    }

    public ComplaintPriority getPriority() {
        return priority;
    }

    public void setPriority(ComplaintPriority priority) {
        this.priority = priority;
    }
}
