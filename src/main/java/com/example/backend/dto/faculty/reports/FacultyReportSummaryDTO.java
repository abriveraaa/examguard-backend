package com.example.backend.dto.faculty.reports;

import java.io.Serializable;

public record FacultyReportSummaryDTO(
        Double averageScore,
        Double submissionRate,
        Long totalViolations,
        Long pendingReview,
        Long penalized
) implements Serializable {}
