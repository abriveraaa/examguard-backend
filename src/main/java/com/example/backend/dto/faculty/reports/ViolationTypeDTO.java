package com.example.backend.dto.faculty.reports;

import java.io.Serializable;

public record ViolationTypeDTO(
        Long examId,
        String examTitle,
        String violationType,
        Long count
) implements Serializable {}
