package com.anurag.campusfix.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anurag.campusfix.dto.response.HealthResponse;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse response = new HealthResponse();
        response.setService("campusfix-backend");

        if (dataSource == null) {
            response.setSuccess(false);
            response.setStatus("degraded");
            response.setDatabase("disconnected");
            response.setError("DataSource bean is not available");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            if (!valid) {
                response.setSuccess(false);
                response.setStatus("degraded");
                response.setDatabase("disconnected");
                response.setError("Database connection validation failed");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }

            response.setDatabase("connected");
            String catalog = connection.getCatalog();
            response.setCatalog(catalog);

            // Verify table readability
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) {
                    response.setUserCount(rs.getLong(1));
                    response.setTables("ready");
                }
            } catch (Exception tableEx) {
                log.warn("Health check table check notice: {}", tableEx.getMessage());
                response.setTables("not_ready");
                response.setError("Users table verification note: " + tableEx.getMessage());
            }

            response.setSuccess(true);
            response.setStatus("healthy");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            response.setSuccess(false);
            response.setStatus("degraded");
            response.setDatabase("disconnected");
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
