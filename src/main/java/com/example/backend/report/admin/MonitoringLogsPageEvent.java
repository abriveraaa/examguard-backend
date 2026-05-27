package com.example.backend.report.admin;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

public class MonitoringLogsPageEvent extends PdfPageEventHelper {

    private final String generatedByText;
    private PdfTemplate totalPageTemplate;
    private BaseFont regularFont;
    private BaseFont boldFont;

    public MonitoringLogsPageEvent(String generatedByText) {
        this.generatedByText = generatedByText == null ? "" : generatedByText;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            totalPageTemplate = writer.getDirectContent().createTemplate(30, 16);
            regularFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            boldFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize monitoring log PDF page event.", e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = writer.getPageSize();

        float y = 24f;

        canvas.beginText();
        canvas.setFontAndSize(regularFont, 8f);
        canvas.setTextMatrix(document.leftMargin(), y);
        canvas.showText(generatedByText);
        canvas.endText();

        int currentPage = writer.getPageNumber();

        String left = "Page ";
        String pageNo = String.valueOf(currentPage);
        String right = " of ";

        float x = page.getRight() - document.rightMargin() - 78f;

        canvas.beginText();

        canvas.setFontAndSize(regularFont, 8f);
        canvas.setTextMatrix(x, y);
        canvas.showText(left);
        x += regularFont.getWidthPoint(left, 8f);

        canvas.setFontAndSize(boldFont, 8f);
        canvas.setTextMatrix(x, y);
        canvas.showText(pageNo);
        x += boldFont.getWidthPoint(pageNo, 8f);

        canvas.setFontAndSize(regularFont, 8f);
        canvas.setTextMatrix(x, y);
        canvas.showText(right);
        x += regularFont.getWidthPoint(right, 8f);

        canvas.endText();

        canvas.addTemplate(totalPageTemplate, x, y);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalPageTemplate.beginText();
        totalPageTemplate.setFontAndSize(boldFont, 8f);
        totalPageTemplate.setTextMatrix(0, 0);
        totalPageTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPageTemplate.endText();
    }
}