package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthResponse {

    private boolean success;
    private String service = "campusfix-backend";
    private String status;
    private String database;
    private String catalog;
    private String jdbcUrl;
    private List<String> availableDatabases;
    private String tables;
    private Long userCount;
    private String error;
    private LocalDateTime timestamp = LocalDateTime.now();

    public HealthResponse() {
    }

    public HealthResponse(boolean success, String status, String database) {
        this.success = success;
        this.service = "campusfix-backend";
        this.status = status;
        this.database = database;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public List<String> getAvailableDatabases() {
        return availableDatabases;
    }

    public void setAvailableDatabases(List<String> availableDatabases) {
        this.availableDatabases = availableDatabases;
    }

    public String getTables() {
        return tables;
    }

    public void setTables(String tables) {
        this.tables = tables;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
