package com.example.backend.report.exam.dto;

import com.example.backend.entity.enums.QuestionType;

import java.math.BigDecimal;

public record ReportQuestionDTO(
        Long questionId,
        Integer questionOrder,
        QuestionType questionType,
        String questionText,
        String questionInstruction,
        String correctAnswer,
        BigDecimal points
) {}