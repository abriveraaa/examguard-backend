package com.example.backend.report.faculty;

import com.example.backend.report.common.ReportConfig;
import com.example.backend.report.common.ReportPdfContext;
import com.example.backend.report.common.ReportPdfUtil;
import java.io.ByteArrayOutputStream;
import com.example.backend.report.exam.dto.*;
import com.example.backend.report.model.ReportRequest;
import com.example.backend.repository.report.ReportRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExamResultSummaryService {

    private final ReportRepository reportRepository;

    public ExamResultSummaryService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public byte[] generatePdf(ReportRequest request) {

        try {
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Long examId = request.getExamId();
            String classOfferingId = request.getClassOfferingId();

            ReportExamHeaderDTO header =
                    reportRepository.findExamHeaderForReport(
                            examId,
                            classOfferingId
                    );

            List<String> sections =
                    reportRepository.findAssignedSectionsForReport(
                                    examId,
                                    classOfferingId
                            )
                            .stream()
                            .sorted()
                            .toList();

            String collegeOffering =
                    header.collegeOffering() == null || header.collegeOffering().isBlank()
                            ? ""
                            : header.collegeOffering();

            ReportPdfContext context =
                    ReportPdfUtil.createFrontPageOnlyHeaderContext(
                            outputStream,
                            ReportConfig.portrait(
                                    collegeOffering,
                                    request.getGeneratedByText()
                            )
                    );

            Document document = context.document();
            PdfWriter writer = context.writer();

            addCoverContent(
                    document,
                    writer,
                    header,
                    sections
            );

            addExamResultSummaryPage(
                    document,
                    examId,
                    classOfferingId,
                    header.totalPoints()
            );

            addQuestionAnalysisPage(
                    document,
                    examId,
                    classOfferingId
            );

            addStudentPerformancePage(
                    document,
                    examId,
                    classOfferingId,
                    header.totalPoints(),
                    header.timeLimitMinutes()
            );

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to generate exam result summary PDF.",
                    e
            );
        }
    }

    private Paragraph centered(
            String text,
            Font font
    ) {
        Paragraph paragraph =
                new Paragraph(text, font);

        paragraph.setAlignment(
                Element.ALIGN_CENTER
        );

        return paragraph;
    }

    private void addCoverContent(
            Document document,
            PdfWriter writer,
            ReportExamHeaderDTO header,
            List<String> sections
    ) throws Exception {

        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = document.getPageSize();

        float centerY = page.getHeight() / 2f;

        Font reportTitleFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);

        Font examFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);

        Font courseFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        Paragraph reportTitle = new Paragraph(
                "EXAM RESULT SUMMARY",
                reportTitleFont
        );
        reportTitle.setAlignment(Element.ALIGN_CENTER);

        ColumnText reportColumn = new ColumnText(canvas);
        reportColumn.setSimpleColumn(
                72f,
                centerY + 80f,
                page.getWidth() - 72f,
                centerY + 125f
        );
        reportColumn.addElement(reportTitle);
        reportColumn.go();

        String examLine = safe(header.examTitle()).toUpperCase();

        String courseLine =
                safe(header.courseCode()).toUpperCase()
                        + " — "
                        + safe(header.courseDescription()).toUpperCase();

        Paragraph examParagraph = new Paragraph(examLine, examFont);
        examParagraph.setAlignment(Element.ALIGN_CENTER);

        ColumnText examColumn = new ColumnText(canvas);
        examColumn.setSimpleColumn(
                72f,
                centerY + 25f,
                page.getWidth() - 72f,
                centerY + 80f
        );
        examColumn.addElement(examParagraph);
        examColumn.go();

        Paragraph courseParagraph = new Paragraph(courseLine, courseFont);
        courseParagraph.setAlignment(Element.ALIGN_CENTER);

        ColumnText courseColumn = new ColumnText(canvas);
        courseColumn.setSimpleColumn(
                72f,
                centerY - 10f,
                page.getWidth() - 72f,
                centerY + 40f
        );
        courseColumn.addElement(courseParagraph);
        courseColumn.go();

        Font labelFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        Font valueFont =
                FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{0.9f, 2.6f});

        addInfoRow(infoTable, "Faculty:", header.faculty(), labelFont, valueFont);
        addInfoRow(infoTable, "Duration:", formatDuration(header.timeLimitMinutes()), labelFont, valueFont);
        addInfoRow(infoTable, "Schedule:", formatSchedule(header.startDateTime(), header.endDateTime()), labelFont, valueFont);
        addInfoRow(infoTable, "Total Points:", formatPoints(header.totalPoints()), labelFont, valueFont);
        addInfoRow(infoTable, "Section/s:", String.join(", ", sections), labelFont, valueFont);

        ColumnText infoColumn = new ColumnText(canvas);
        infoColumn.setSimpleColumn(
                130f,
                145f,
                page.getWidth() - 130f,
                245f
        );
        infoColumn.addElement(infoTable);
        infoColumn.go();
    }

    private void addExamResultSummaryPage(
            Document document,
            Long examId,
            String classOfferingId,
            BigDecimal totalPoints
    ) throws DocumentException {

        document.newPage();

        ExamResultSummaryMetricsDTO metrics =
                mapMetrics(
                        reportRepository.findExamResultSummaryMetricsRaw(
                                examId,
                                classOfferingId
                        )
                );

        List<ScoreDistributionDTO> distribution =
                reportRepository.findScoreDistributionRaw(examId, classOfferingId)
                        .stream()
                        .map(this::mapScoreDistribution)
                        .toList();

        List<ViolationSummaryDTO> violations =
                reportRepository.findViolationSummaryRaw(examId, classOfferingId)
                        .stream()
                        .map(this::mapViolationSummary)
                        .toList();

        Paragraph title = centered("OVERALL PERFORMANCE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        title.setSpacingAfter(10f);
        document.add(title);

        addMetricCards(document,metrics,totalPoints);

        Paragraph distributionTitle =sectionTitle("SCORE DISTRIBUTION");
        distributionTitle.setSpacingBefore(10f);
        document.add(distributionTitle);
        addScoreDistributionBars(document,distribution);

        Paragraph violationTitle = sectionTitle("VIOLATION SUMMARY");
        violationTitle.setSpacingBefore(12f);
        document.add(violationTitle);
        addViolationSummaryTable(document, violations);
        addViolationSummaryBars(document, violations);

    }

    private void addQuestionAnalysisPage(
            Document document,
            Long examId,
            String classOfferingId
    ) throws DocumentException {

        document.newPage();

        List<QuestionAnalysisDTO> questions =reportRepository.findQuestionAnalysisRaw(examId, classOfferingId)
                        .stream()
                        .map(this::mapQuestionAnalysis)
                        .toList();

        Paragraph title = centered("QUESTION ANALYSIS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        title.setSpacingAfter(10f);
        document.add(title);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new float[]{0.45f, 0.8f, 3.6f, 0.9f, 0.9f, 1f, 1f});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7.2f);

        addTableHeader(table, "#", headerFont);
        addTableHeader(table, "Type", headerFont);
        addTableHeader(table, "Question", headerFont);
        addTableHeader(table, "Correct", headerFont);
        addTableHeader(table, "Incorrect", headerFont);
        addTableHeader(table, "Correct %", headerFont);
        addTableHeader(table, "Difficulty", headerFont);

        for (QuestionAnalysisDTO q : questions) {
            addTableCell(table, String.valueOf(q.questionOrder()), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, formatQuestionType(q.questionType()), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, shorten(q.questionText(), 120), bodyFont, Element.ALIGN_LEFT);
            addTableCell(table, String.valueOf(q.correctCount()), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, String.valueOf(q.incorrectCount()), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, formatPercentage(q.correctPercentage()), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, q.difficulty(), bodyFont, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void addStudentPerformancePage(
            Document document,
            Long examId,
            String classOfferingId,
            BigDecimal totalPoints,
            Integer timeLimitMinutes
    ) throws DocumentException {

        document.newPage();

        List<ReportStudentSummaryDTO> students =
                reportRepository.findStudentSummariesForReport(
                        examId,
                        classOfferingId
                );

        List<ReportStudentViolationDTO> violations =
                reportRepository.findStudentViolationsForReport(
                        examId,
                        classOfferingId
                );

        List<ReportStudentDurationDTO> durations =
                reportRepository.findStudentDurationsForReport(
                        examId,
                        classOfferingId
                );

        var violationsByStudentId =
                violations.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                ReportStudentViolationDTO::studentId,
                                java.util.stream.Collectors.mapping(
                                        ReportStudentViolationDTO::violationType,
                                        java.util.stream.Collectors.joining(", ")
                                )
                        ));

        var durationMsByAttemptId =
                durations.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                ReportStudentDurationDTO::attemptId,
                                ReportStudentDurationDTO::totalDurationMs
                        ));

        Paragraph title = centered( "STUDENT PERFORMANCE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        title.setSpacingAfter(10f);
        document.add(title);

        PdfPTable table = new PdfPTable(7);
        table.setWidths(new float[]{0.45f, 1.35f, 2.2f, 1.2f, 1.2f, 1.4f, 0.9f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7.2f);

        addTableHeader(table, "#", headerFont);
        addTableHeader(table, "Student ID", headerFont);
        addTableHeader(table, "Name", headerFont);
        addTableHeader(table, "Score", headerFont);
        addTableHeader(table, "Status", headerFont);
        addTableHeader(table, "Violations", headerFont);
        addTableHeader(table, "Duration", headerFont);

        int rank = 1;

        for (ReportStudentSummaryDTO student : students) {
            String violationText =
                    violationsByStudentId.getOrDefault(student.studentId(), "None");

            addTableCell(table, String.valueOf(rank++), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, safe(student.studentId()), bodyFont, Element.ALIGN_LEFT);
            addTableCell(table, safe(student.studentName()), bodyFont, Element.ALIGN_LEFT);
            addTableCell(table, formatScore(student, totalPoints), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, formatStatus(student.attemptStatus()), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, formatViolationDisplay(violationText), bodyFont, Element.ALIGN_CENTER);
            addTableCell(
                    table,
                    formatDurationFromMs(student, durationMsByAttemptId, timeLimitMinutes),
                    bodyFont,
                    Element.ALIGN_CENTER
            );
        }

        document.add(table);
    }

    private void addMetricCards(
            Document document,
            ExamResultSummaryMetricsDTO metrics,
            BigDecimal totalPoints
    ) throws DocumentException {

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f, 1f, 1f});
        table.setSpacingAfter(8f);

        addMetricCard(table, "Assigned", String.valueOf(metrics.assignedStudents()));
        addMetricCard(table, "Submitted", String.valueOf(metrics.submitted()));
        addMetricCard(table, "Did Not Take", String.valueOf(metrics.didNotTake()));
        addMetricCard(table, "Submission Rate", formatPercentage(metrics.submissionRate()));

        addMetricCard(table, "Average Score", formatScoreValue(metrics.averageScore(), totalPoints, metrics.averagePercentage()));
        addMetricCard(table, "Highest Score", formatScoreValue(metrics.highestScore(), totalPoints, metrics.highestPercentage()));
        addMetricCard(table, "Lowest Score", formatScoreValue(metrics.lowestScore(), totalPoints, metrics.lowestPercentage()));
        addMetricCard(table, "Passing Rate", formatPercentage(metrics.passingRate()));

        document.add(table);
    }

    private void addBarRow(
            PdfPTable table,
            String label,
            long value,
            long max,
            Color color
    ) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f);

        float percent = max <= 0 ? 0f : (float) value / (float) max;

        addTableCellNoBorder(table, label, labelFont, Element.ALIGN_LEFT);
        table.addCell(createBarCell(percent, color));
        addTableCellNoBorder(table, String.valueOf(value), valueFont, Element.ALIGN_RIGHT);
    }

    private void addMetricCard(
            PdfPTable table,
            String label,
            String value
    ) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(7f);
        cell.setMinimumHeight(42f);
        cell.setBackgroundColor(new Color(248, 248, 248));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph labelP = new Paragraph(label, labelFont);
        labelP.setAlignment(Element.ALIGN_CENTER);
        labelP.setSpacingAfter(3f);

        Paragraph valueP = new Paragraph(value, valueFont);
        valueP.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(labelP);
        cell.addElement(valueP);

        table.addCell(cell);
    }

    private void addScoreDistributionBars(
            Document document,
            List<ScoreDistributionDTO> distribution
    ) throws DocumentException {

        long max = distribution.stream()
                .mapToLong(d -> d.studentCount() == null ? 0 : d.studentCount())
                .max()
                .orElse(1);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 5f, 0.7f});
        table.setSpacingAfter(8f);

        Font labelFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f);

        Font countFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f);

        for (ScoreDistributionDTO row : distribution) {
            long count = row.studentCount() == null ? 0 : row.studentCount();

            float percent =
                    max <= 0
                            ? 0f
                            : (float) count / (float) max;

            addTableCellNoBorder(
                    table,
                    row.rangeLabel(),
                    labelFont,
                    Element.ALIGN_LEFT
            );

            table.addCell(createBarCell(percent, new Color(128, 0, 0)));

            addTableCellNoBorder(
                    table,
                    String.valueOf(count),
                    countFont,
                    Element.ALIGN_RIGHT
            );
        }

        document.add(table);
    }

    private PdfPCell createBarCell(
            float percent,
            Color fillColor
    ) {
        PdfPCell cell = new PdfPCell();

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(20f);
        cell.setPadding(0f);

        cell.setCellEvent((pdfPCell, rectangle, canvases) -> {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];

            float x = rectangle.getLeft() + 2f;
            float y = rectangle.getBottom() + 5f;
            float width = rectangle.getWidth() - 4f;
            float height = 8f;

            canvas.saveState();

            canvas.setColorFill(new Color(235, 235, 235));
            canvas.rectangle(x, y, width, height);
            canvas.fill();

            canvas.setColorFill(fillColor);
            canvas.rectangle(x, y, width * percent, height);
            canvas.fill();

            canvas.restoreState();
        });

        return cell;
    }

    private void addViolationSummaryBars(
            Document document,
            List<ViolationSummaryDTO> violations
    ) throws DocumentException {

        if (violations == null || violations.isEmpty()) {
            return;
        }

        long max = violations.stream()
                .mapToLong(v -> v.violationCount() == null ? 0 : v.violationCount())
                .max()
                .orElse(1);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 4f, 0.8f});
        table.setSpacingAfter(8f);

        for (ViolationSummaryDTO violation : violations) {
            addBarRow(
                    table,
                    formatViolationType(violation.violationType()),
                    violation.violationCount(),
                    max,
                    new Color(198, 40, 40)
            );
        }

        document.add(table);
    }

    private void addViolationSummaryTable(
            Document document,
            List<ViolationSummaryDTO> violations
    ) throws DocumentException {

        if (violations == null || violations.isEmpty()) {
            Paragraph none = new Paragraph(
                    "No violations recorded.",
                    FontFactory.getFont(FontFactory.HELVETICA, 8.5f)
            );
            none.setSpacingAfter(6f);
            document.add(none);
            return;
        }

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new float[]{2.5f, 1f, 1f});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);

        addTableHeader(table, "Violation Type", headerFont);
        addTableHeader(table, "Count", headerFont);
        addTableHeader(table, "Affected Students", headerFont);

        for (ViolationSummaryDTO violation : violations) {
            addTableCell(table, formatViolationType(violation.violationType()), bodyFont, Element.ALIGN_LEFT);
            addTableCell(table, String.valueOf(violation.violationCount()), bodyFont, Element.ALIGN_CENTER);
            addTableCell(table, String.valueOf(violation.affectedStudents()), bodyFont, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private ExamResultSummaryMetricsDTO mapMetrics(Object[] raw) {
        if (raw != null && raw.length == 1 && raw[0] instanceof Object[] nested) {
            raw = nested;
        }

        return new ExamResultSummaryMetricsDTO(
                toLong(raw[0]),
                toLong(raw[1]),
                toLong(raw[2]),
                toLong(raw[3]),
                toDouble(raw[4]),
                toBigDecimal(raw[5]),
                toDouble(raw[6]),
                toBigDecimal(raw[7]),
                toDouble(raw[8]),
                toBigDecimal(raw[9]),
                toDouble(raw[10]),
                toDouble(raw[11]),
                toLong(raw[12])
        );
    }

    private ScoreDistributionDTO mapScoreDistribution(Object[] raw) {
        return new ScoreDistributionDTO(
                String.valueOf(raw[0]),
                toLong(raw[1])
        );
    }

    private ViolationSummaryDTO mapViolationSummary(Object[] raw) {
        return new ViolationSummaryDTO(
                String.valueOf(raw[0]),
                toLong(raw[1]),
                toLong(raw[2])
        );
    }

    private QuestionAnalysisDTO mapQuestionAnalysis(Object[] raw) {
        return new QuestionAnalysisDTO(
                toLong(raw[0]),
                toInteger(raw[1]),
                String.valueOf(raw[2]),
                String.valueOf(raw[3]),
                toLong(raw[4]),
                toLong(raw[5]),
                toLong(raw[6]),
                toDouble(raw[7]),
                String.valueOf(raw[8])
        );
    }

    private void addInfoRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {

        PdfPCell labelCell =
                new PdfPCell(new Phrase(label, labelFont));

        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPaddingBottom(4f);
        labelCell.setPaddingRight(4f);

        PdfPCell valueCell =
                new PdfPCell(new Phrase(value, valueFont));

        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setPaddingBottom(4f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Paragraph sectionTitle(String text) {
        Paragraph paragraph = new Paragraph(
                text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
        );
        paragraph.setSpacingAfter(5f);
        return paragraph;
    }

    private void addTableHeader(
            PdfPTable table,
            String text,
            Font font
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBackgroundColor(new Color(235, 235, 235));
        table.addCell(cell);
    }

    private void addTableCell(
            PdfPTable table,
            String text,
            Font font,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private void addTableCellNoBorder(
            PdfPTable table,
            String text,
            Font font,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(3f);
        table.addCell(cell);
    }

    private String formatScore(
            ReportStudentSummaryDTO student,
            BigDecimal totalPoints
    ) {
        return formatPercentage(student.scorePercentage())
                + " ("
                + formatPoints(student.totalScore())
                + "/"
                + formatPoints(totalPoints)
                + ")";
    }

    private String formatScoreValue(
            BigDecimal score,
            BigDecimal totalPoints,
            Double percentage
    ) {
        return formatPoints(score)
                + "/"
                + formatPoints(totalPoints)
                + " ("
                + formatPercentage(percentage)
                + ")";
    }

    private String formatPoints(BigDecimal points) {
        if (points == null) return "0";
        return points.stripTrailingZeros().toPlainString();
    }

    private String formatPoints(Double value) {
        if (value == null) {
            return "0";
        }

        if (value % 1 == 0) {
            return String.valueOf(value.intValue());
        }

        return String.format("%.2f", value);
    }

    private String formatPercentage(Double value) {
        if (value == null) {
            return "0%";
        }

        if (value % 1 == 0) {
            return value.intValue() + "%";
        }

        return String.format("%.2f%%", value);
    }

    private String formatStatus(Object status) {
        if (status == null) {
            return "DID NOT TAKE";
        }

        return status.toString().replace("_", " ");
    }

    private String formatDuration(Integer totalMinutes) {
        if (totalMinutes == null || totalMinutes <= 0) {
            return "0 minutes";
        }

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours)
                    .append(" hour")
                    .append(hours > 1 ? "s" : "");
        }

        if (minutes > 0) {
            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(minutes)
                    .append(" minute")
                    .append(minutes > 1 ? "s" : "");
        }

        return result.toString();
    }

    private String formatSchedule(
            OffsetDateTime start,
            OffsetDateTime end
    ) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

        if (start == null || end == null) {
            return "";
        }

        return start.format(formatter)
                + " - "
                + end.format(formatter);
    }

    private String formatDurationFromMs(
            ReportStudentSummaryDTO student,
            java.util.Map<Long, Long> durationMsByAttemptId,
            Integer timeLimitMinutes
    ) {
        if (student.attemptId() == null) {
            return "-";
        }

        long totalMs =
                durationMsByAttemptId.getOrDefault(student.attemptId(), 0L);

        if (totalMs <= 0) {
            return "-";
        }

        long totalSeconds = totalMs / 1000;

        long maxSeconds =
                timeLimitMinutes == null || timeLimitMinutes <= 0
                        ? totalSeconds
                        : timeLimitMinutes * 60L;

        totalSeconds = Math.min(totalSeconds, maxSeconds);

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder result = new StringBuilder();

        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0 || result.isEmpty()) result.append(seconds).append("s");

        return result.toString().trim();
    }

    private String formatViolationDisplay(String violationText) {
        if (violationText == null
                || violationText.isBlank()
                || "None".equalsIgnoreCase(violationText)) {
            return "None";
        }

        String[] parts = violationText.split(",");

        if (parts.length == 1) {
            return formatViolationType(parts[0]);
        }

        return "Multiple Violations";
    }

    private String formatViolationType(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replace("_", " ");
    }

    private String formatQuestionType(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("MULTIPLE_CHOICE", "MCQ")
                .replace("TRUE_FALSE", "T/F")
                .replace("IDENTIFICATION", "ID")
                .replace("ESSAY", "ESSAY");
    }

    private String shorten(String value, int maxLength) {
        String safe = safe(value);

        if (safe.length() <= maxLength) {
            return safe;
        }

        return safe.substring(0, maxLength - 3) + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long v) return v;
        if (value instanceof Integer v) return v.longValue();
        if (value instanceof BigInteger v) return v.longValue();
        if (value instanceof Number v) return v.longValue();
        return Long.parseLong(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer v) return v;
        if (value instanceof Number v) return v.intValue();
        return Integer.parseInt(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double v) return v;
        if (value instanceof Float v) return v.doubleValue();
        if (value instanceof BigDecimal v) return v.doubleValue();
        if (value instanceof Number v) return v.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal v) return v;
        if (value instanceof Number v) return BigDecimal.valueOf(v.doubleValue());
        return new BigDecimal(value.toString());
    }

}