package com.example.backend.dto.faculty.reports;

import java.io.Serializable;

public record SubmissionStatusDTO(
        String status,
        Long count
) implements Serializable {}