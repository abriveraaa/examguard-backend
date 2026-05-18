package com.example.backend.report.exam.dto;

import java.math.BigDecimal;

public record ReportRubricDTO(
        Long questionId,
        String criteriaName,
        BigDecimal percentage
) {}