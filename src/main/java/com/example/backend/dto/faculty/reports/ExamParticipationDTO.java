package com.example.backend.dto.faculty.reports;

import java.io.Serializable;

public record ExamParticipationDTO(
        Long examId,
        String examTitle,
        Long totalTakers,
        Double averageScore
) implements Serializable {}
