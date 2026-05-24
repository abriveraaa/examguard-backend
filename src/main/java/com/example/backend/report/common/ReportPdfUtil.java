package com.example.backend.report.common;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

public class ReportPdfUtil {

    public static Document createDocument(
            ByteArrayOutputStream outputStream,
            ReportConfig config
    ) throws Exception {

        Document document = new Document(
                config.getPageSize(),
                35f,
                35f,
                120f,
                125f
        );

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        writer.setPageEvent(new ReportPageEvent(
                config.getCollegeOffering(),
                config.getGeneratedByText()
        ));

        document.open();

        return document;
    }

    public static ReportPdfContext createFrontPageOnlyHeaderContext(
            ByteArrayOutputStream outputStream,
            ReportConfig config
    ) throws Exception {

        Document document = new Document(
                config.getPageSize(),
                35f,
                35f,
                45f,
                65f
        );

        PdfWriter writer =
                PdfWriter.getInstance(document, outputStream);

        writer.setPageEvent(new ReportFrontPageOnlyEvent(
                config.getCollegeOffering(),
                config.getGeneratedByText()
        ));

        document.open();

        return new ReportPdfContext(document, writer);
    }
}