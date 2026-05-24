package com.example.backend.report.common;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

public class ReportFrontPageOnlyEvent extends ReportPageEvent {

    private PdfTemplate totalPageTemplate;
    private BaseFont regularFont;
    private BaseFont boldFont;

    public ReportFrontPageOnlyEvent(String collegeOffering, String generatedByText) {
        super(collegeOffering, generatedByText);
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        super.onOpenDocument(writer, document);

        try {
            totalPageTemplate = writer.getDirectContent().createTemplate(30, 16);
            regularFont = BaseFont.createFont( BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED );
            boldFont = BaseFont.createFont( BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize page numbering.", e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        int currentPage = writer.getPageNumber();

        if (currentPage == 1) {
            super.onEndPage(writer, document);
            return;
        }

        drawSucceedingPageNumber(writer);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        try {
            totalPageTemplate.beginText();
            totalPageTemplate.setFontAndSize(boldFont, 8f);
            totalPageTemplate.setTextMatrix(0, 0);
            totalPageTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPageTemplate.endText();

        } catch (Exception e) {
            throw new RuntimeException("Unable to write total page count.", e);
        }
    }

    private void drawSucceedingPageNumber(PdfWriter writer) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = writer.getPageSize();

        float y = 36f;
        float x = page.getRight() - 120f;

        String left = "Page ";
        String middle = String.valueOf(writer.getPageNumber());
        String right = " of ";

        canvas.beginText();

        canvas.setFontAndSize(regularFont, 8f);
        canvas.setTextMatrix(x, y);
        canvas.showText(left);
        x += regularFont.getWidthPoint(left, 8f);

        canvas.setFontAndSize(boldFont, 8f);
        canvas.setTextMatrix(x, y);
        canvas.showText(middle);
        x += boldFont.getWidthPoint(middle, 8f);

        canvas.setFontAndSize(regularFont, 8f);
        canvas.setTextMatrix(x, y);
        canvas.showText(right);
        x += regularFont.getWidthPoint(right, 8f);

        canvas.endText();

        canvas.addTemplate(totalPageTemplate, x, y);
    }
}