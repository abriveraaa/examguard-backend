package com.example.backend.report.factory;

import com.example.backend.report.base.PdfReportExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportExporterFactory {

    private final List<PdfReportExporter> exporters;

    public PdfReportExporter getExporter(String reportType) {
        return exporters.stream()
                .filter(exporter -> exporter.supports(reportType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported report type: " + reportType));
    }
}