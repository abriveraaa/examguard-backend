package com.example.backend.report.admin;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.dto.admin.monitoring.AdminMonitoringLogsRequest;
import com.example.backend.entity.core.UserAccess;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MonitoringLogsExcelExporter {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    public byte[] export(
            List<AdminLogRowDto> rows,
            AdminMonitoringLogsRequest request,
            UserAccess user
    ) {
        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet(resolveTabName(request.getSource()));

            CellStyle titleStyle = titleStyle(workbook);
            CellStyle subtitleStyle = subtitleStyle(workbook);
            CellStyle metaStyle = metaStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);

            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ExamGuard | Log Report");
            titleCell.setCellStyle(titleStyle);

            Row subtitleRow = sheet.createRow(rowIndex++);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue(resolveTabName(request.getSource()));
            subtitleCell.setCellStyle(subtitleStyle);

            rowIndex++;

            Row generatedRow = sheet.createRow(rowIndex++);
            Cell generatedCell = generatedRow.createCell(0);
            generatedCell.setCellValue(buildGeneratedByText(user));
            generatedCell.setCellStyle(metaStyle);

            Row filterRow = sheet.createRow(rowIndex++);
            Cell filterCell = filterRow.createCell(0);
            filterCell.setCellValue(buildFilterSummary(request));
            filterCell.setCellStyle(metaStyle);

            rowIndex++;

            String source = normalizeSource(request.getSource());

            String[] headers = headersForSource(source);

            Row headerRow = sheet.createRow(rowIndex++);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (AdminLogRowDto log : rows) {
                Row row = sheet.createRow(rowIndex++);

                Object[] values = valuesForSource(source, log);

                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(value(values[i]));
                    cell.setCellStyle(bodyStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 6000);
            }

            sheet.createFreezePane(0, 6);

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export monitoring logs Excel.", e);
        }
    }

    private String[] headersForSource(String source) {
        return switch (source) {
            case "VIOLATION" -> new String[]{
                    "Time",
                    "Student",
                    "Course",
                    "Exam",
                    "Question #",
                    "Violation Type",
                    "Severity",
                    "Status",
                    "Message"
            };

            case "SYSTEM" -> new String[]{
                    "Time",
                    "Actor",
                    "Role",
                    "Target User",
                    "Target Role",
                    "Module",
                    "Action",
                    "Status",
                    "Duration",
                    "Message"
            };

            case "SESSION" -> new String[]{
                    "Started At",
                    "User",
                    "Role",
                    "Device",
                    "IP Address",
                    "Status",
                    "Last Seen",
                    "Message"
            };

            case "ACCESS" -> new String[]{
                    "Time",
                    "Actor",
                    "Role",
                    "Event",
                    "Status",
                    "Message"
            };

            case "ACCOUNT" -> new String[]{
                    "Time",
                    "Actor",
                    "Role",
                    "Action",
                    "Status",
                    "Target User",
                    "Target Role",
                    "Message"
            };

            case "REGISTRAR" -> new String[]{
                    "Time",
                    "Actor",
                    "Sync Type",
                    "Status",
                    "Message"
            };

            case "CAMERA" -> new String[]{
                    "Time",
                    "Student",
                    "Exam ID",
                    "Attempt ID",
                    "Device",
                    "Status",
                    "Message"
            };

            default -> new String[]{
                    "Time",
                    "Source",
                    "Actor",
                    "Role",
                    "Target User",
                    "Target Role",
                    "Module",
                    "Action",
                    "Status",
                    "Duration",
                    "Message"
            };
        };
    }

    private Object[] valuesForSource(String source, AdminLogRowDto log) {
        return switch (source) {
            case "VIOLATION" -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getActorId(),
                    log.getCourseCode(),
                    log.getExamTitle(),
                    log.getQuestionNumber(),
                    log.getAction(),
                    log.getSeverity(),
                    log.getStatus(),
                    log.getMessage()
            };

            case "SYSTEM" -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getActorId(),
                    log.getActorRole(),
                    log.getTargetUserId(),
                    log.getTargetRole(),
                    log.getModule(),
                    log.getAction(),
                    log.getStatus(),
                    formatDuration(log.getDurationMs()),
                    log.getMessage()
            };

            case "SESSION" -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getActorId(),
                    log.getActorRole(),
                    log.getExamTitle(),
                    log.getCourseCode(),
                    log.getStatus(),
                    formatDate(log.getEndedAt()),
                    log.getMessage()
            };

            case "ACCESS" -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getActorId(),
                    log.getActorRole(),
                    log.getAction(),
                    log.getStatus(),
                    log.getMessage()
            };

            case "ACCOUNT" -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getActorId(),
                    log.getActorRole(),
                    log.getAction(),
                    log.getStatus(),
                    log.getTargetUserId(),
                    log.getTargetRole(),
                    log.getMessage()
            };

            case "REGISTRAR" -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getActorId(),
                    log.getAction(),
                    log.getStatus(),
                    log.getMessage()
            };

            case "CAMERA" -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getActorId(),
                    log.getExamId(),
                    log.getAttemptId(),
                    log.getAction(),
                    log.getStatus(),
                    log.getMessage()
            };

            default -> new Object[]{
                    formatDate(log.getStartedAt()),
                    log.getSource(),
                    log.getActorId(),
                    log.getActorRole(),
                    log.getTargetUserId(),
                    log.getTargetRole(),
                    log.getModule(),
                    log.getAction(),
                    log.getStatus(),
                    formatDuration(log.getDurationMs()),
                    log.getMessage()
            };
        };
    }

    private CellStyle titleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_RED.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);

        return style;
    }

    private CellStyle subtitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);

        return style;
    }

    private CellStyle metaStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 9);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);

        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle bodyStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 9);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private String buildGeneratedByText(UserAccess user) {
        String actor = user == null
                ? "Unknown"
                : safe(user.getSchoolId()) + " | " + safe(user.getUsername());

        String generatedAt = java.time.ZonedDateTime.now(MANILA)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return "Generated By: " + actor + " | " + generatedAt;
    }

    private String buildFilterSummary(AdminMonitoringLogsRequest request) {
        if (request == null) {
            return "Range: - | Search: - | Role: - | Status: - | Action: - | Module: - | Severity: - | Violation Type: -";
        }

        return "Range: " + safe(request.getRange()) +
                " | Search: " + safe(request.getSearch()) +
                " | Role: " + safe(request.getRole()) +
                " | Status: " + safe(request.getStatus()) +
                " | Action: " + safe(request.getAction()) +
                " | Module: " + safe(request.getModule()) +
                " | Severity: " + safe(request.getSeverity()) +
                " | Violation Type: " + safe(request.getViolationType());
    }

    private String resolveTabName(String source) {
        return switch (normalizeSource(source)) {
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

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "ALL";
        }

        return source.trim().toUpperCase();
    }

    private String formatDate(OffsetDateTime value) {
        if (value == null) {
            return "-";
        }

        return value.atZoneSameInstant(MANILA)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return "-";
        }

        long totalSeconds = durationMs / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }

        return seconds + "s";
    }

    private String value(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value);

        return text.isBlank() ? "-" : text;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}