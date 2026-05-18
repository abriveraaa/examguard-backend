package com.example.backend.report.exam.dto;

public record ReportEssayRubricScoreDTO(
        Long attemptId,
        Long questionId,
        String criterionName,
        Double questionPoints,
        Double weightPercentage,
        Double scoreAwarded,
        Double scorePercentage,
        String feedback
) {}