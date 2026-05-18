package com.example.backend.report.base;

import com.example.backend.report.model.ReportRequest;

public interface PdfReportExporter {

    byte[] export(ReportRequest request);

    boolean supports(String reportType);
}