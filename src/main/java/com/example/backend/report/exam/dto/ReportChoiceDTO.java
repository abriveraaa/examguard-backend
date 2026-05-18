package com.example.backend.report.exam.dto;

public record ReportChoiceDTO(
        Long questionId,
        Long choiceId,
        String choiceLabel,
        String choiceText,
        Boolean correct
) {}