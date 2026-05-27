package com.example.backend.report.admin;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.dto.admin.monitoring.AdminMonitoringLogsRequest;
import com.example.backend.entity.core.UserAccess;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MonitoringLogsPdfExporter {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    public byte[] export(
            List<AdminLogRowDto> rows,
            AdminMonitoringLogsRequest request,
            UserAccess user
    ) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document(
                    PageSize.LETTER.rotate(),
                    28f,
                    28f,
                    36f,
                    42f
            );

            PdfWriter writer = PdfWriter.getInstance(document, out);

            writer.setPageEvent(
                    new MonitoringLogsPageEvent(
                            buildGeneratedByText(user)
                    )
            );

            document.open();

            addTitle(document, request);
            addFilterSummary(document, request);
            addTable(document, rows, request);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export monitoring logs PDF.", e);
        }
    }

    private void addTitle(Document document, AdminMonitoringLogsRequest request) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(48, 44, 41));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(128, 0, 0));

        Paragraph title = new Paragraph("ExamGuard | Log Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4f);
        document.add(title);

        Paragraph subtitle = new Paragraph(resolveTabName(request.getSource()), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(12f);
        document.add(subtitle);
    }

    private void addFilterSummary(Document document, AdminMonitoringLogsRequest request) throws Exception {
        Font filterFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(90, 84, 78));

        String summary =
                "Range: " + safe(request.getRange()) +
                        " | Search: " + safe(request.getSearch()) +
                        " | Role: " + safe(request.getRole()) +
                        " | Status: " + safe(request.getStatus()) +
                        " | Action: " + safe(request.getAction()) +
                        " | Module: " + safe(request.getModule()) +
                        " | Severity: " + safe(request.getSeverity());

        Paragraph p = new Paragraph(summary, filterFont);
        p.setSpacingAfter(8f);
        document.add(p);
    }

    private void addTable(
            Document document,
            List<AdminLogRowDto> rows,
            AdminMonitoringLogsRequest request
    ) throws Exception {

        String source = request.getSource() == null ? "ALL" : request.getSource().toUpperCase();

        PdfPTable table;

        if ("VIOLATION".equals(source)) {
            table = new PdfPTable(new float[]{1.35f, 1.3f, 1.1f, 1.7f, .7f, 1.2f, 1f, 1f, 2.2f});
            table.setWidthPercentage(100);
            addHeader(table, "Time", "Student", "Course", "Exam", "Q#", "Type", "Severity", "Status", "Message");

            for (AdminLogRowDto row : rows) {
                addCell(table, formatDate(row.getStartedAt()));
                addCell(table, row.getActorId());
                addCell(table, row.getCourseCode());
                addCell(table, row.getExamTitle());
                addCell(table, row.getQuestionNumber());
                addCell(table, row.getAction());
                addCell(table, row.getSeverity());
                addCell(table, row.getStatus());
                addCell(table, row.getMessage());
            }

        } else if ("SYSTEM".equals(source)) {
            table = new PdfPTable(new float[]{1.35f, 1.2f, .9f, 1.2f, 1.2f, 1f, 2.6f, .8f});
            table.setWidthPercentage(100);
            addHeader(table, "Time", "Actor", "Role", "Module", "Action", "Status", "Message", "Duration");

            for (AdminLogRowDto row : rows) {
                addCell(table, formatDate(row.getStartedAt()));
                addCell(table, row.getActorId());
                addCell(table, row.getActorRole());
                addCell(table, row.getModule());
                addCell(table, row.getAction());
                addCell(table, row.getStatus());
                addCell(table, row.getMessage());
                addCell(table, formatDuration(row.getDurationMs()));
            }

        } else {
            table = new PdfPTable(new float[]{1.25f, .9f, 1.15f, .9f, 1.2f, 1.2f, .9f, 2.7f});
            table.setWidthPercentage(100);
            addHeader(table, "Time", "Source", "Actor", "Role", "Module", "Action", "Status", "Message");

            for (AdminLogRowDto row : rows) {
                addCell(table, formatDate(row.getStartedAt()));
                addCell(table, row.getSource());
                addCell(table, row.getActorId());
                addCell(table, row.getActorRole());
                addCell(table, row.getModule());
                addCell(table, row.getAction());
                addCell(table, row.getStatus());
                addCell(table, row.getMessage());
            }
        }

        document.add(table);
    }

    private void addHeader(PdfPTable table, String... headers) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new Color(128, 0, 0));
            cell.setPadding(5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table, Object value) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 7f, new Color(48, 44, 41));

        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : String.valueOf(value), font));
        cell.setPadding(4f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setBorderColor(new Color(220, 212, 204));
        table.addCell(cell);
    }

    private String resolveTabName(String source) {
        if (source == null) return "All Logs";

        return switch (source.toUpperCase()) {
            case "VIOLATION" -> "Violation Logs";
            case "SYSTEM" -> "System Logs";
            case "SESSION" -> "Session Logs";
            case "ACCESS" -> "Access Logs";
            case "ACCOUNT" -> "Account Logs";
            case "REGISTRAR" -> "Registrar Sync Logs";
            case "CAMERA" -> "Camera Sessions";
            default -> "All Logs";
        };
    }

    private String buildGeneratedByText(UserAccess user) {
        String actor = user == null
                ? "Unknown"
                : safe(user.getSchoolId()) + " | " + safe(user.getUsername());

        String generatedAt = java.time.ZonedDateTime.now(MANILA)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return "Generated By: " + actor + " | " + generatedAt;
    }

    private String formatDate(java.time.OffsetDateTime value) {
        if (value == null) return "-";

        return value.atZoneSameInstant(MANILA)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null || durationMs <= 0) return "-";

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }

        return seconds + "s";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}