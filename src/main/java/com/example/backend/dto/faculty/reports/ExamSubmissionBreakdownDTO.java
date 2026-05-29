package com.example.backend.dto.faculty.reports;

import java.io.Serializable;

public record ExamSubmissionBreakdownDTO(
        Long examId,
        String examTitle,
        String status,
        Long count
) implements Serializable {}