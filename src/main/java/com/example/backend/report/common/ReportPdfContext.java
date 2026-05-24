package com.example.backend.report.common;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;

public record ReportPdfContext(
        Document document,
        PdfWriter writer
) {
}