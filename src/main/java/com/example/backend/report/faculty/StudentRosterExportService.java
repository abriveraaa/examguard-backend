package com.example.backend.report.faculty;

import com.example.backend.dto.faculty.students.FacultyStudentDTO;
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
import java.util.List;

@Service
public class StudentRosterExportService {

    public byte[] generatePdf(
            List<FacultyStudentDTO> students,
            String collegeOffering,
            String generatedByText
    ) {
        try {
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document = ReportPdfUtil.createDocument(outputStream, ReportConfig.portrait(collegeOffering, generatedByText));

            addTitle(document);
            addRosterTable(document, students);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to generate student roster PDF.",
                    e
            );
        }
    }

    private void addTitle(Document document) throws Exception {
        Font titleFont =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        15
                );

        Paragraph title =
                new Paragraph(
                        "STUDENT ROSTER",
                        titleFont
                );

        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(18);

        document.add(title);
    }

    private void addRosterTable(
            Document document,
            List<FacultyStudentDTO> students
    ) throws Exception {
        PdfPTable table = new PdfPTable(7);

        table.setWidthPercentage(100);
        table.setWidths(new float[]{
                2.2f,
                3.6f,
                4.0f,
                3.2f,
                2.4f,
                1.2f,
                1.5f
        });

        addHeader(table, "Student No.");
        addHeader(table, "Student Name");
        addHeader(table, "Email");
        addHeader(table, "College");
        addHeader(table, "Program");
        addHeader(table, "Year");
        addHeader(table, "Section");

        if (students == null || students.isEmpty()) {
            PdfPCell emptyCell =
                    new PdfPCell(
                            new Phrase("No students found.")
                    );

            emptyCell.setColspan(7);
            emptyCell.setPadding(10);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            table.addCell(emptyCell);
        } else {
            for (FacultyStudentDTO student : students) {
                addBodyCell(table, student.studentId());
                addBodyCell(table, student.lastName() + ", " + student.firstName());
                addBodyCell(table, student.emailAddress());
                addBodyCell(table, student.collegeName());
                addBodyCell(table, student.programCode());
                addBodyCell(table, String.valueOf(student.yearLevel()));
                addBodyCell(table, student.sectionName());
            }
        }

        document.add(table);
    }

    private void addHeader(
            PdfPTable table,
            String text
    ) {
        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        8,
                        Color.WHITE
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(text, font)
                );

        cell.setBackgroundColor(new Color(128, 0, 0));
        cell.setPadding(7);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String text
    ) {
        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        8,
                        Color.BLACK
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text == null || text.isBlank()
                                        ? "-"
                                        : text,
                                font
                        )
                );

        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }
}