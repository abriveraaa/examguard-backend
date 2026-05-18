package com.example.backend.report.exam.dto;

import java.time.OffsetDateTime;

public record ReportStudentSummaryDTO(
        Long attemptId,
        String studentId,
        String studentName,
        String programCode,
        String faculty,
        Double totalScore,
        Double scorePercentage,
        String attemptStatus,
        OffsetDateTime startedAt,
        OffsetDateTime submittedAt
) {}