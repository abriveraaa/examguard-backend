package com.example.backend.report.exam.dto;

import java.math.BigDecimal;

public record ExamResultSummaryMetricsDTO(
        Long assignedStudents,
        Long submitted,
        Long autoSubmitted,
        Long didNotTake,
        Double submissionRate,
        BigDecimal averageScore,
        Double averagePercentage,
        BigDecimal highestScore,
        Double highestPercentage,
        BigDecimal lowestScore,
        Double lowestPercentage,
        Double passingRate,
        Long withViolations
) {
}