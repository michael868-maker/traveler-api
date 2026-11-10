package com.traveler.traveler_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(){
        Map<String, Object> database = new LinkedHashMap<>();
        boolean healthy;
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            healthy = true;
        } catch (Exception e) {
            healthy = false;
        }
        long responseTime = System.currentTimeMillis() - start;

        Map<String, Object> body = new LinkedHashMap<>();
        if (healthy) {
            database.put("status", "healthy");
            body.put("status", "healthy");
        } else {
            database.put("status", "unhealthy");
            body.put("status", "unhealthy");
        }
        database.put("responseTime", responseTime);
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        body.put("database", database);

        if (healthy) {
            return ResponseEntity.ok(body);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}