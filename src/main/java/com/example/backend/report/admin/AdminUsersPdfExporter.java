package com.example.backend.report.admin;

import com.example.backend.dto.admin.users.AdminUsersExportRequest;
import com.example.backend.dto.admin.users.AdminUserRowDto;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.report.common.ReportPageEvent;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AdminUsersPdfExporter {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    public byte[] export(
            List<AdminUserRowDto> rows,
            AdminUsersExportRequest request,
            UserAccess user
    ) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document(
                    PageSize.LETTER.rotate(),
                    36f,
                    36f,
                    125f,
                    125f
            );

            PdfWriter writer = PdfWriter.getInstance(document, out);

            writer.setPageEvent(new ReportPageEvent(
                    null,
                    buildGeneratedByText(user)
            ));

            document.open();

            addTitle(document, request);
            addFilterSummary(document, request);
            addTable(document, rows, request);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export admin users PDF.", e);
        }
    }

    private void addTitle(Document document, AdminUsersExportRequest request) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(48, 44, 41));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(128, 0, 0));

        Paragraph title = new Paragraph("ExamGuard | User Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4f);
        document.add(title);

        Paragraph subtitle = new Paragraph(request.getRole() + " ROSTER", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(12f);
        document.add(subtitle);
    }

    private void addFilterSummary(Document document, AdminUsersExportRequest request) throws Exception {

        Font font = FontFactory.getFont(
                FontFactory.HELVETICA,
                8,
                new Color(90, 84, 78)
        );

        String text =
                "Role: " + safe(request.getRole()) +
                        " | Status: " + safe(request.getStatus()) +
                        " | Keyword: " + safe(request.getKeyword()) +
                        " | Reactivation: " + safe(request.getReactivation());

        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(8f);

        document.add(p);
    }

    private boolean isStudentReport(AdminUsersExportRequest request) {
        return request.getRole() != null
                && request.getRole().equalsIgnoreCase("STUDENT");
    }

    private void addTable(
            Document document,
            List<AdminUserRowDto> rows,
            AdminUsersExportRequest request
    ) throws Exception {

        boolean studentReport = isStudentReport(request);

        PdfPTable table = studentReport
                ? new PdfPTable(new float[]{
                1.2f, 1.0f, 1.5f, 1.8f,
                .8f, .9f, .7f,
                1.0f, 1.0f
        })
                : new PdfPTable(new float[]{
                1.3f, 1.2f, 1.8f, 2.2f,
                1.1f, 1.1f
        });

        table.setWidthPercentage(100);
        table.setSplitLate(false);
        table.setSplitRows(true);

        if (studentReport) {
            addHeader(
                    table,
                    "School ID", "Username", "Full Name", "Email",
                    "College", "Program", "Year", "Registrar Status", "System Access"
            );
        } else {
            addHeader(
                    table,
                    "School ID", "Username", "Full Name", "Email",
                    "Registrar Status", "System Access"
            );
        }

        for (AdminUserRowDto row : rows) {
            addCell(table, row.getSchoolId());
            addCell(table, row.getUsername());
            addCell(table, row.getFullName());
            addCell(table, row.getEmail());

            if (studentReport) {
                addCell(table, row.getCollegeCode());
                addCell(table, row.getProgramCode());
                addCell(table, row.getYearLevel());
            }

            addCell(table, row.getRegistrarStatus());
            addCell(table, row.getSystemAccess());
        }

        document.add(table);
    }

    private void addHeader(PdfPTable table, String... headers) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, Color.WHITE);


        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new Color(128, 0, 0));
            cell.setPadding(4f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setNoWrap(false);
            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table, Object value) {
        Font font = FontFactory.getFont(
                FontFactory.HELVETICA,
                6.5f,
                new Color(48, 44, 41)
        );

        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : String.valueOf(value), font));
        cell.setPadding(3f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(new Color(220, 212, 204));
        cell.setNoWrap(false);

        table.addCell(cell);
    }

    private String buildGeneratedByText(UserAccess user) {
        String actor = user == null
                ? "Unknown"
                : safe(user.getSchoolId()) + " | " + safe(user.getUsername());

        String generatedAt = java.time.ZonedDateTime.now(MANILA)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return "Generated By: " + actor + " | " + generatedAt;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}