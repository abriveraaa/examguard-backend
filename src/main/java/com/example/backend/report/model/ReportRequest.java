package com.example.backend.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReportRequest {
    private String classOfferingId;
    private Long examId;
    private String studentId;
    private ReportType reportType;
    private String generatedByText;
}