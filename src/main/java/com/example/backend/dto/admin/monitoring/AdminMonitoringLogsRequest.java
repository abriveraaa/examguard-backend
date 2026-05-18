package com.example.backend.dto.admin.monitoring;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AdminMonitoringLogsRequest {

    private int page = 0;
    private int size = 20;

    private String source = "ALL";
    // ALL, SYSTEM, VIOLATION

    private String search;
    private String role;
    private String severity;

    private String range;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    private String academicYear;
    private String term;
}