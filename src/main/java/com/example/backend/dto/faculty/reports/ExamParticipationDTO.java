package com.example.backend.dto.faculty.reports;

public record ExamParticipationDTO(
        Long examId,
        String examTitle,
        Long totalTakers,
        Double averageScore
) {}
