package com.example.backend.dto.faculty.reports;

public record ExamSubmissionBreakdownDTO(
        Long examId,
        String examTitle,
        String status,
        Long count
) {}