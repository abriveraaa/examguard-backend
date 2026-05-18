package com.example.backend.report.exam.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ReportExamHeaderDTO(
        Long examId,
        String examTitle,
        String courseCode,
        String courseDescription,
        String faculty,
        String collegeOffering,
        Integer timeLimitMinutes,
        OffsetDateTime startDateTime,
        OffsetDateTime endDateTime,
        BigDecimal totalPoints
) {}