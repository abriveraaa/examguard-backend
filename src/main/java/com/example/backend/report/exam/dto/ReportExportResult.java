package com.example.backend.report.exam.dto;

import org.springframework.http.MediaType;

public record ReportExportResult(
        byte[] bytes,
        String fileName,
        MediaType contentType
) {
}