package com.anurag.campusfix.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getRoot() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "CampusFix Backend API",
                "status", "UP",
                "health", "/api/health",
                "frontend", "https://campusfix-phi.vercel.app"
        ));
    }
}
