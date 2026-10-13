package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import com.anurag.campusfix.entity.ComplaintImageEntity;

public class ComplaintImageResponse {

    private Long id;
    private String storageKey;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String url;

    public ComplaintImageResponse() {
    }

    public static ComplaintImageResponse fromEntity(ComplaintImageEntity entity) {
        if (entity == null) {
            return null;
        }
        ComplaintImageResponse res = new ComplaintImageResponse();
        res.setId(entity.getId());
        res.setStorageKey(entity.getStorageKey());
        res.setOriginalFilename(entity.getOriginalFilename());
        res.setContentType(entity.getContentType());
        res.setFileSize(entity.getFileSize());
        res.setUploadedAt(entity.getUploadedAt());
        if (entity.getComplaint() != null) {
            res.setUrl("/api/complaints/" + entity.getComplaint().getId() + "/images/" + entity.getId());
        }
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
