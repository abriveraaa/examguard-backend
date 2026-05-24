package com.example.backend.report.exam.dto;

public record QuestionAnalysisDTO(
        Long questionId,
        Integer questionOrder,
        String questionType,
        String questionText,
        Long correctCount,
        Long incorrectCount,
        Long totalAnswered,
        Double correctPercentage,
        String difficulty
) {
}