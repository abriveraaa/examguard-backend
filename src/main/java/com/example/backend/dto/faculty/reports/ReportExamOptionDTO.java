package com.example.backend.dto.faculty.reports;

import java.io.Serializable;

public record ReportExamOptionDTO(
        Long examId,
        String title,
        Long classOfferingCount
) implements Serializable {}
