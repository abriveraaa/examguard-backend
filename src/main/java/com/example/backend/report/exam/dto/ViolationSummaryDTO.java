package com.example.backend.report.exam.dto;

public record ViolationSummaryDTO(
        String violationType,
        Long violationCount,
        Long affectedStudents
) {
}