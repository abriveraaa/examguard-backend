package com.example.backend.report.exam.dto;

import com.example.backend.entity.enums.QuestionType;

public record ReportStudentAnswerDTO(
        Long attemptId,
        String studentId,
        String studentName,
        Long questionId,
        Integer questionOrder,
        QuestionType questionType,
        String questionText,
        String questionInstruction,
        String correctAnswer,
        Long selectedChoiceId,
        String selectedChoiceText,
        String answerText,
        Boolean isCorrect,
        Double earnedPoints,
        Double questionPoints,
        String facultyFeedback
) {}