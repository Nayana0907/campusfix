package com.anurag.campusfix.dto.request;

import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ComplaintCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private ComplaintCategory category;

    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    @NotBlank(message = "Building is required")
    @Size(max = 100, message = "Building name must not exceed 100 characters")
    private String building;

    @NotBlank(message = "Floor is required")
    @Size(max = 50, message = "Floor must not exceed 50 characters")
    private String floor;

    @NotBlank(message = "Room is required")
    @Size(max = 50, message = "Room must not exceed 50 characters")
    private String room;

    public ComplaintCreateRequest() {
    }

    public ComplaintCreateRequest(String title, String description, ComplaintCategory category,
                                  ComplaintPriority priority, String building, String floor, String room) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority != null ? priority : ComplaintPriority.MEDIUM;
        this.building = building;
        this.floor = floor;
        this.room = room;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public ComplaintPriority getPriority() {
        return priority != null ? priority : ComplaintPriority.MEDIUM;
    }

    public void setPriority(ComplaintPriority priority) {
        this.priority = priority;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}
