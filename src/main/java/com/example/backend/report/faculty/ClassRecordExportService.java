package com.example.backend.report.faculty;

import com.example.backend.dto.faculty.reports.ClassRecordColumnDTO;
import com.example.backend.dto.faculty.reports.ClassRecordScoreCellDTO;
import com.example.backend.dto.faculty.reports.ClassRecordStudentRowDTO;
import com.example.backend.report.common.ReportConfig;
import com.example.backend.report.common.ReportPdfUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ClassRecordExportService {

    private static final ZoneId MANILA_ZONE =
            ZoneId.of("Asia/Manila");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");


    // ==============
    // PDF GENERATION
    // ==============

    public byte[] generatePdf(
            String courseCode,
            List<String> sectionNames,
            List<ClassRecordColumnDTO> columns,
            List<ClassRecordStudentRowDTO> rows,
            String collegeOffering,
            String generatedByText
    ) {
        try {
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    ReportPdfUtil.createDocument(
                            outputStream,
                            ReportConfig.portrait(
                                    collegeOffering,
                                    generatedByText
                            )
                    );

            addTitle(document,courseCode,sectionNames);
            addClassRecordTable(document,columns,rows);

            document.newPage();

            addExamLegend(document,columns);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Unable to generate class record PDF.",  e );
        }
    }


    // ==============
    // METHOD HELPER
    // ==============

    private void addTitle(
            Document document,
            String course,
            List<String> sectionNames
    ) throws Exception {

        Font titleFont = FontFactory.getFont( FontFactory.HELVETICA_BOLD, 15);
        Paragraph title = new Paragraph( "CLASS RECORD", titleFont );
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6);

        Font subtitleFont = FontFactory.getFont( FontFactory.HELVETICA_BOLD, 13);
        String sectionsText = sectionNames == null || sectionNames.isEmpty() ? "-" : String.join(", ", sectionNames);
        Paragraph fullCourse = new Paragraph( safe(course), subtitleFont);
        Paragraph fullSection = new Paragraph( safe(sectionsText), subtitleFont);
        fullCourse.setAlignment(Element.ALIGN_CENTER);
        fullCourse.setSpacingAfter(14);
        fullSection.setAlignment(Element.ALIGN_CENTER);
        fullSection.setSpacingAfter(14);

        document.add(title);
        document.add(fullCourse);
        document.add(fullSection);

    }

    private void addClassRecordTable(
            Document document,
            List<ClassRecordColumnDTO> columns,
            List<ClassRecordStudentRowDTO> rows
    ) throws Exception {
        int examCount =
                columns == null
                        ? 0
                        : columns.size();

        int fixedColumns = 2;
        int totalColumns = fixedColumns + examCount + 1;

        PdfPTable table =
                new PdfPTable(totalColumns);

        table.setWidthPercentage(100);

        float[] widths =
                new float[totalColumns];

        widths[0] = 2.3f; // Student No.
        widths[1] = 4.2f; // Student Name

        for (int i = 2; i < totalColumns - 1; i++) {
            widths[i] = 1.4f; // EXAM columns
        }

        widths[totalColumns - 1] = 1.7f; // Average

        table.setWidths(widths);

        addHeader(table, "Student No.");
        addHeader(table, "Student Name");

        if (columns != null) {
            for (ClassRecordColumnDTO column : columns) {
                addHeader(
                        table,
                        column.examCode()
                );
            }
        }

        addHeader(table, "Average");

        if (rows == null || rows.isEmpty()) {
            PdfPCell emptyCell =
                    new PdfPCell(
                            new Phrase("No students found.")
                    );

            emptyCell.setColspan(totalColumns);
            emptyCell.setPadding(10);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            table.addCell(emptyCell);

            document.add(table);
            return;
        }

        for (ClassRecordStudentRowDTO row : rows) {
            addBodyCell(
                    table,
                    row.studentId(),
                    Element.ALIGN_LEFT
            );

            addBodyCell(
                    table,
                    row.studentName(),
                    Element.ALIGN_LEFT
            );

            if (columns != null) {
                for (ClassRecordColumnDTO column : columns) {
                    ClassRecordScoreCellDTO scoreCell =
                            row.scoresByExamId()
                                    .get(column.examId());

                    addBodyCell( table, formatScoreCell(scoreCell), Element.ALIGN_CENTER
                    );
                }
            }

            addBodyCell(
                    table,
                    formatPercent(row.averagePercentage()),
                    Element.ALIGN_CENTER
            );
        }

        document.add(table);
    }

    private void addExamLegend(
            Document document,
            List<ClassRecordColumnDTO> columns
    ) throws Exception {
        Paragraph title = new Paragraph( "EXAM LEGEND", FontFactory.getFont( FontFactory.HELVETICA_BOLD, 10));

        title.setSpacingBefore(16);
        title.setSpacingAfter(8);

        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidths(new float[]{1.2f,5.2f,1.5f,2.5f,2.5f});

        addHeader(table, "Code");
        addHeader(table, "Exam Title");
        addHeader(table, "Points");
        addHeader(table, "Start");
        addHeader(table, "End");

        if (columns == null || columns.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell( new Phrase("No exams found.") );

            emptyCell.setColspan(5);
            emptyCell.setPadding(10);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            table.addCell(emptyCell);

            document.add(table);
            return;
        }

        for (ClassRecordColumnDTO column : columns) {
            addBodyCell(
                    table,
                    column.examCode(),
                    Element.ALIGN_CENTER
            );

            addBodyCell(
                    table,
                    column.examTitle(),
                    Element.ALIGN_LEFT
            );

            addBodyCell(
                    table,
                    formatNumber(column.totalPoints()),
                    Element.ALIGN_CENTER
            );

            addBodyCell(
                    table,
                    formatDateTime(column.startDateTime()),
                    Element.ALIGN_CENTER
            );

            addBodyCell(
                    table,
                    formatDateTime(column.endDateTime()),
                    Element.ALIGN_CENTER
            );
        }

        document.add(table);
    }

    private String formatPercent(
            BigDecimal value
    ) {
        if (value == null) { return "-"; }

        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String formatScoreCell(
            ClassRecordScoreCellDTO cell
    ) {
        if (cell == null) {
            return "0% (0/0)";
        }

        if ("DID_NOT_TAKE".equalsIgnoreCase(cell.status())) {
            return "0% (0/" + formatNumber(cell.totalPoints()) + ")";
        }

        if ("PENDING".equalsIgnoreCase(cell.status())) {
            return "-";
        }

        if (cell.percentage() == null) {
            return "-";
        }

        return formatPercentWithScore(
                cell.percentage(),
                cell.score(),
                cell.totalPoints()
        );
    }

    private String formatPercentWithScore(
            BigDecimal percentage,
            BigDecimal score,
            BigDecimal totalPoints
    ) {
        if (percentage == null) {
            return "-";
        }

        return percentage.setScale(0, RoundingMode.HALF_UP).toPlainString()
                + "% ("
                + formatNumber(score)
                + "/"
                + formatNumber(totalPoints)
                + ")";
    }

    private String formatNumber(
            BigDecimal value
    ) {
        if (value == null) {
            return "0";
        }

        return value.stripTrailingZeros().toPlainString();
    }

    private String formatDateTime(
            OffsetDateTime value
    ) {
        if (value == null) {
            return "-";
        }

        return value
                .atZoneSameInstant(MANILA_ZONE)
                .format(DATE_TIME_FORMATTER);
    }

    private void addHeader( PdfPTable table, String text ) {
        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7,
                        Color.WHITE
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safe(text),
                                font
                        )
                );


        cell.setBackgroundColor(new Color(128, 0, 0));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String text,
            int alignment
    ) {
        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7,
                        Color.BLACK
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safe(text),
                                font
                        )
                );

        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private String safe(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }
}