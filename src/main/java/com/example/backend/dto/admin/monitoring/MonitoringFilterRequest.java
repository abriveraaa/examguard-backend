package com.example.backend.dto.admin.monitoring;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MonitoringFilterRequest {

    private String academicYear;
    private String term;

    private String range;
    // Entire Period, Entire Term, This Month, Today, Custom Range

    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    private String groupBy;
    // Auto, Year, Month, Day, Hour

    private String programCode;
    private String collegeCode;
    private String role;
}