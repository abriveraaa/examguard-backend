package com.example.backend.report.exam.dto;

public record ReportStudentDurationDTO(
        Long attemptId,
        Long totalDurationMs
) {}