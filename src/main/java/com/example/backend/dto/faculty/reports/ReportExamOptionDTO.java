package com.example.backend.dto.faculty.reports;

public record ReportExamOptionDTO(
        Long examId,
        String title,
        Long classOfferingCount
) {}
