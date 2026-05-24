package com.example.backend.dto.faculty.reports;

public record FacultyReportSummaryDTO(
        Double averageScore,
        Double submissionRate,
        Long totalViolations,
        Long pendingReview,
        Long penalized
) {}
