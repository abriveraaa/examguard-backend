package com.example.backend.report.base;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

public abstract class AbstractPdfReportExporter implements PdfReportExporter {

    protected byte[] buildPdf(
            PdfBuilderCallback callback,
            PdfPageEventHelper pageEvent
    ) {
        try {
            Document document = new Document(
                    PageSize.LETTER,
                    72f,   // left   = 2.54 cm
                    72f,   // right  = 2.54 cm
                    31.5f, // top    = 1.11 cm
                    49.6f  // bottom = 1.75 cm
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = PdfWriter.getInstance(document, out);

            if (pageEvent != null) {
                writer.setPageEvent(pageEvent);
            }

            document.open();

            callback.build(document, writer);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report.", e);
        }
    }

    protected Font font(float size, int style) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }

    protected Font titleFont() {
        return font(18, Font.BOLD);
    }

    protected Font subtitleFont() {
        return font(13, Font.BOLD);
    }

    protected Font normalFont() {
        return font(10, Font.NORMAL);
    }

    protected Paragraph centered(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    protected PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, normalFont()));
        cell.setPadding(7);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    protected PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, subtitleFont()));
        cell.setPadding(7);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    protected PdfPTable table(float... widths) throws DocumentException {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        return table;
    }

    @FunctionalInterface
    protected interface PdfBuilderCallback {
        void build(Document document, PdfWriter writer) throws Exception;
    }
}