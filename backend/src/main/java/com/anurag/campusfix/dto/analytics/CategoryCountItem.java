package com.anurag.campusfix.dto.analytics;

import com.anurag.campusfix.entity.ComplaintCategory;

public class CategoryCountItem {

    private ComplaintCategory category;
    private long count;

    public CategoryCountItem() {
    }

    public CategoryCountItem(ComplaintCategory category, long count) {
        this.category = category;
        this.count = count;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
