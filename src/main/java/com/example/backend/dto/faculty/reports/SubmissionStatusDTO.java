package com.example.backend.dto.faculty.reports;

public record SubmissionStatusDTO(
        String status,
        Long count
) {}