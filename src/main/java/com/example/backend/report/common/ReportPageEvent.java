package com.example.backend.report.common;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.InputStream;

public class ReportPageEvent extends PdfPageEventHelper {

    private final String collegeOffering;
    private final String generatedByText;

    private PdfTemplate totalPageTemplate;
    private BaseFont regularFont;
    private BaseFont boldFont;

    public ReportPageEvent(
            String collegeOffering,
            String generatedByText
    ) {
        this.collegeOffering = collegeOffering == null || collegeOffering.isBlank()
                ? " "
                : collegeOffering.toUpperCase();

        this.generatedByText = generatedByText == null
                ? ""
                : generatedByText;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        totalPageTemplate = writer.getDirectContent().createTemplate(30, 16);

        try {
            regularFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            boldFont = BaseFont.createFont(BaseFont.TIMES_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load PDF fonts.", e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        drawHeader(writer);
        drawFooter(writer);
        drawPageNumber(writer);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        try {
            BaseFont bold = BaseFont.createFont(
                    getClass().getResource("/fonts/NotoSans-Bold.ttf").toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );

            totalPageTemplate.beginText();
            totalPageTemplate.setFontAndSize(bold, 8f);
            totalPageTemplate.setTextMatrix(0, 0);
            totalPageTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPageTemplate.endText();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void drawHeader(PdfWriter writer) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = writer.getPageSize();

        float top = page.getTop();

        try {
            Image pupLogo = loadImage("/report-assets/pup-logo.png");
            pupLogo.scaleAbsolute(72f, 73.4f);
            pupLogo.setAbsolutePosition(35f, top - 95f);
            canvas.addImage(pupLogo);

            Image bagong = loadImage("/report-assets/bagong-pilipinas.png");
            bagong.scaleAbsolute(82.8f, 82.8f);
            bagong.setAbsolutePosition(page.getRight() - 35f - 82.8f, top - 98f);
            canvas.addImage(bagong);

        } catch (Exception ignored) {
        }

        float textX = 118f;

        writeText(canvas, "REPUBLIC OF THE PHILIPPINES", textX, top - 41f, 9f, false);

        writeMixedPupTitle(
                canvas,
                "POLYTECHNIC UNIVERSITY OF THE PHILIPPINES",
                textX,
                top - 55f
        );

        writeText(
                canvas,
                "OFFICE OF THE VICE PRESIDENT FOR ACADEMIC AFFAIRS",
                textX,
                top - 69f,
                10f,
                false
        );

        writeText(
                canvas,
                collegeOffering,
                textX,
                top - 85f,
                13f,
                true
        );

        canvas.saveState();
        canvas.setColorStroke(Color.GRAY);
        canvas.setLineWidth(1f);
        float lineY = page.getTop() - 107f;

        canvas.moveTo(35f, lineY);
        canvas.lineTo(page.getRight() - 35f, lineY);
        canvas.stroke();
        canvas.restoreState();
    }

    private void drawFooter(PdfWriter writer) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = writer.getPageSize();

        float footerTextX = 72f;
        float taglineY = 58f;

        writeTextWithFont(
                canvas,
                "PUP A. Mabini Campus, Anonas Street, Sta. Mesa, Manila 1016",
                footerTextX,
                102f,
                8f,
                BaseFont.HELVETICA,
                false
        );

        writeTextWithFont(
                canvas,
                "Trunk Line: 335-1787 or 335-1777",
                footerTextX,
                91f,
                8f,
                BaseFont.HELVETICA,
                false
        );

        writeTextWithFont(
                canvas,
                "Website: www.pup.edu.ph | Inquiries: https://bit.ly/PUPSINTA",
                footerTextX,
                80f,
                8f,
                BaseFont.HELVETICA,
                false
        );

        writeMixedPolytechnicFooter(
                canvas,
                "THE COUNTRY'S 1st POLYTECHNIC U",
                footerTextX,
                taglineY
        );

        try {
            Image socotec = loadImage("/report-assets/socotec.png");
            socotec.scaleAbsolute(93f, 83.6f);
            socotec.setAbsolutePosition(page.getRight() - 72f - 93f, 48f);
            canvas.addImage(socotec);
        } catch (Exception ignored) {
        }
    }

    private void drawPageNumber(PdfWriter writer) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = writer.getPageSize();

        int currentPage = writer.getPageNumber();

        try {
            BaseFont regular = BaseFont.createFont(
                    getClass().getResource("/fonts/NotoSans-Regular.ttf").toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );

            BaseFont bold = BaseFont.createFont(
                    getClass().getResource("/fonts/NotoSans-Bold.ttf").toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );

            float y = 36f;

            canvas.beginText();
            canvas.setFontAndSize(regular, 7f);
            canvas.setTextMatrix(36f, y);
            canvas.showText(generatedByText);
            canvas.endText();

            float x = page.getRight() - 120f;

            String left = "Page ";
            String middle = String.valueOf(currentPage);
            String right = " of ";

            canvas.beginText();

            canvas.setFontAndSize(regular, 8f);
            canvas.setTextMatrix(x, y);
            canvas.showText(left);
            x += regular.getWidthPoint(left, 8f);

            canvas.setFontAndSize(bold, 8f);
            canvas.setTextMatrix(x, y);
            canvas.showText(middle);
            x += bold.getWidthPoint(middle, 8f);

            canvas.setFontAndSize(regular, 8f);
            canvas.setTextMatrix(x, y);
            canvas.showText(right);
            x += regular.getWidthPoint(right, 8f);

            canvas.endText();

            canvas.addTemplate(totalPageTemplate, x, y);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeText(
            PdfContentByte canvas,
            String text,
            float x,
            float y,
            float size,
            boolean bold
    ) {
        canvas.beginText();
        canvas.setFontAndSize(bold ? boldFont : regularFont, size);
        canvas.setTextMatrix(x, y);
        canvas.showText(text == null ? "" : text);
        canvas.endText();
    }

    private void writeTextWithFont(
            PdfContentByte canvas,
            String text,
            float x,
            float y,
            float size,
            String baseFontName,
            boolean bold
    ) {
        try {
            BaseFont font = BaseFont.createFont(
                    bold ? BaseFont.HELVETICA_BOLD : baseFontName,
                    BaseFont.WINANSI,
                    BaseFont.NOT_EMBEDDED
            );

            canvas.beginText();
            canvas.setFontAndSize(font, size);
            canvas.setTextMatrix(x, y);
            canvas.showText(text == null ? "" : text);
            canvas.endText();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeMixedPolytechnicFooter(
            PdfContentByte canvas,
            String text,
            float x,
            float y
    ) {
        try {
            BaseFont regular = BaseFont.createFont(
                    BaseFont.TIMES_ROMAN,
                    BaseFont.WINANSI,
                    BaseFont.NOT_EMBEDDED
            );

            float normalSize = 15f;
            float firstLetterSize = 17f;

            float currentX = x;

            for (int i = 0; i < text.length(); i++) {
                String s = String.valueOf(text.charAt(i));

                boolean firstLetter =
                        i == 0 ||
                                (i > 0 && text.charAt(i - 1) == ' ' && Character.isLetter(text.charAt(i)));

                float size = firstLetter ? firstLetterSize : normalSize;

                canvas.beginText();
                canvas.setFontAndSize(regular, size);
                canvas.setTextMatrix(currentX, y);
                canvas.showText(s);
                canvas.endText();

                currentX += regular.getWidthPoint(s, size);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeMixedPupTitle(
            PdfContentByte canvas,
            String text,
            float x,
            float y
    ) {
        canvas.beginText();

        try {
            BaseFont bold = BaseFont.createFont(
                    BaseFont.TIMES_BOLD,
                    BaseFont.WINANSI,
                    BaseFont.NOT_EMBEDDED
            );

            float normalSize = 11f;
            float pupSize = 13f;

            float currentX = x;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                String s = String.valueOf(c);

                boolean isPupLetter = false;

                if (i == 0 && s.equals("P")) {
                    isPupLetter = true;
                }

                if (text.startsWith("UNIVERSITY", i) && s.equals("U")) {
                    isPupLetter = true;
                }

                if (text.startsWith("PHILIPPINES", i) && s.equals("P")) {
                    isPupLetter = true;
                }

                float size = isPupLetter ? pupSize : normalSize;

                canvas.setFontAndSize(bold, size);
                canvas.setTextMatrix(currentX, y);
                canvas.showText(s);

                currentX += bold.getWidthPoint(s, size);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        canvas.endText();
    }

    private Image loadImage(String classpathLocation) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing asset: " + classpathLocation);
            }

            byte[] bytes = inputStream.readAllBytes();
            return Image.getInstance(bytes);
        }
    }
}