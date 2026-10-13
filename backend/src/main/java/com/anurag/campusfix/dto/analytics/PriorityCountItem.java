package com.anurag.campusfix.dto.analytics;

import com.anurag.campusfix.entity.ComplaintPriority;

public class PriorityCountItem {

    private ComplaintPriority priority;
    private long count;

    public PriorityCountItem() {
    }

    public PriorityCountItem(ComplaintPriority priority, long count) {
        this.priority = priority;
        this.count = count;
    }

    public ComplaintPriority getPriority() {
        return priority;
    }

    public void setPriority(ComplaintPriority priority) {
        this.priority = priority;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
