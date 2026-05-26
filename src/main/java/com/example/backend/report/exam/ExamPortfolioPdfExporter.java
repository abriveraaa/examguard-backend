package com.example.backend.report.exam;

import com.example.backend.entity.enums.QuestionType;
import com.example.backend.report.base.AbstractPdfReportExporter;
import com.example.backend.report.exam.dto.*;
import com.example.backend.report.exam.renderer.StudentAnswerPdfRenderer;
import com.example.backend.report.model.ReportRequest;
import com.example.backend.repository.report.ReportRepository;
import com.example.backend.entity.exam.ExamAnswerReviewLog;
import com.example.backend.repository.exam.ExamAnswerReviewLogRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ExamPortfolioPdfExporter extends AbstractPdfReportExporter {

    private final ReportRepository reportRepository;
    private final StudentAnswerPdfRenderer studentAnswerPdfRenderer;
    private final ExamAnswerReviewLogRepository examAnswerReviewLogRepository;
    private final Font unicodeFont;

    public ExamPortfolioPdfExporter(ReportRepository reportRepository,
                                    StudentAnswerPdfRenderer studentAnswerPdfRenderer,
                                    ExamAnswerReviewLogRepository examAnswerReviewLogRepository) {
        this.reportRepository = reportRepository;
        this.studentAnswerPdfRenderer = studentAnswerPdfRenderer;
        this.examAnswerReviewLogRepository = examAnswerReviewLogRepository;

        try {
            BaseFont unicodeBaseFont = BaseFont.createFont(
                    Objects.requireNonNull(
                            getClass().getResource("/fonts/NotoSans-Regular.ttf")
                    ).toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );

            this.unicodeFont = new Font(unicodeBaseFont, 8.5f, Font.BOLD);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load Unicode font.", e);
        }
    }

    @Override
    public byte[] export(ReportRequest request) {
        String classOfferingId = request.getClassOfferingId();

        ReportExamHeaderDTO header =
                reportRepository.findExamHeaderForReport(
                        request.getExamId(),
                        classOfferingId
                );

        List<String> sections =
                reportRepository.findAssignedSectionsForReport(
                                request.getExamId(),
                                classOfferingId
                        )
                        .stream()
                        .sorted()
                        .toList();


        String collegeOffering = header.collegeOffering() == null || header.collegeOffering().isBlank()
                ? ""
                : header.collegeOffering();

        return buildPdf((document, writer) -> {
            addCoverContent(document, writer, header, sections);
            addAnswerSheet(document, header, request.getExamId());
            addStudentSummary(
                    document,
                    request.getExamId(),
                    classOfferingId,
                    header.totalPoints(),
                    header.timeLimitMinutes()
            );
            addStudentAnswerSectionCover(document, writer);

            addStudentAnswerPages(
                    document,
                    request.getExamId(),
                    classOfferingId,
                    header
            );

        }, new ExamPortfolioPageEvent(
                collegeOffering,
                request.getGeneratedByText()
        ));
    }


    // HEADER

    private void addCoverContent(
            Document document,
            PdfWriter writer,
            ReportExamHeaderDTO header,
            List<String> sections
    ) throws Exception {

        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = document.getPageSize();

        float centerY = page.getHeight() / 2f;

        Font examFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font courseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);

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
                centerY + 5f,
                page.getWidth() - 72f,
                centerY + 75f
        );
        examColumn.addElement(examParagraph);
        examColumn.go();

        Paragraph courseParagraph = new Paragraph(courseLine, courseFont);
        courseParagraph.setAlignment(Element.ALIGN_CENTER);

        ColumnText courseColumn = new ColumnText(canvas);
        courseColumn.setSimpleColumn(
                72f,
                centerY - 25f,
                page.getWidth() - 72f,
                centerY + 35f
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
                230f
        );
        infoColumn.addElement(infoTable);
        infoColumn.go();
    }

    private void addInfoRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(label, labelFont)
                );

        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPaddingBottom(4f);
        labelCell.setPaddingRight(4f);

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(value, valueFont)
                );

        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setPaddingBottom(4f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // ANSWER SHEET

    private void addAnswerSheet(
            Document document,
            ReportExamHeaderDTO header,
            Long examId
    ) throws DocumentException {

        List<ReportQuestionDTO> questions =
                reportRepository.findQuestionsForReport(examId);

        List<ReportChoiceDTO> choices =
                reportRepository.findChoicesForReport(examId);

        List<ReportRubricDTO> rubrics =
                reportRepository.findRubricsForReport(examId);

        Map<Long, List<ReportChoiceDTO>> choicesByQuestionId =
                choices.stream()
                        .collect(Collectors.groupingBy(ReportChoiceDTO::questionId));

        Map<Long, List<ReportRubricDTO>> rubricsByQuestionId =
                rubrics.stream()
                        .collect(Collectors.groupingBy(ReportRubricDTO::questionId));

        document.newPage();

        Paragraph title = centered(
                "ANSWER KEY",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)
        );
        title.setSpacingAfter(10f);
        document.add(title);

        addQuestionTypeSection(
                document,
                "I. MULTIPLE CHOICE",
                QuestionType.MULTIPLE_CHOICE,
                questions,
                choicesByQuestionId,
                rubricsByQuestionId
        );

        addQuestionTypeSection(
                document,
                "II. TRUE OR FALSE",
                QuestionType.TRUE_FALSE,
                questions,
                choicesByQuestionId,
                rubricsByQuestionId
        );

        addQuestionTypeSection(
                document,
                "III. IDENTIFICATION",
                QuestionType.IDENTIFICATION,
                questions,
                choicesByQuestionId,
                rubricsByQuestionId
        );

        addQuestionTypeSection(
                document,
                "IV. ESSAY",
                QuestionType.ESSAY,
                questions,
                choicesByQuestionId,
                rubricsByQuestionId
        );
    }

    private void addQuestionTypeSection(
            Document document,
            String sectionTitle,
            QuestionType questionType,
            List<ReportQuestionDTO> questions,
            Map<Long, List<ReportChoiceDTO>> choicesByQuestionId,
            Map<Long, List<ReportRubricDTO>> rubricsByQuestionId
    ) throws DocumentException {

        List<ReportQuestionDTO> filtered =
                questions.stream()
                        .filter(q -> q.questionType() == questionType)
                        .toList();

        if (filtered.isEmpty()) {
            return;
        }

        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        Paragraph section = new Paragraph(sectionTitle, sectionFont);
        section.setSpacingBefore(6f);
        section.setSpacingAfter(4f);
        document.add(section);

        int number = 1;

        for (ReportQuestionDTO question : filtered) {
            addQuestionBlock(
                    document,
                    number++,
                    question,
                    choicesByQuestionId.getOrDefault(question.questionId(), List.of()),
                    rubricsByQuestionId.getOrDefault(question.questionId(), List.of())
            );
        }
    }

    private void addQuestionBlock(
            Document document,
            int number,
            ReportQuestionDTO question,
            List<ReportChoiceDTO> choices,
            List<ReportRubricDTO> rubrics
    ) throws DocumentException {

        Font questionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        addAnswerKeyQuestionRow(
                document,
                number + ". " + safe(question.questionText()),
                formatPoints(question.points())
        );

        if (question.questionType() == QuestionType.MULTIPLE_CHOICE) {
            addMultipleChoiceAnswerBlock(document, choices);
        }

        if (question.questionType() == QuestionType.TRUE_FALSE) {
            addSimpleAnswerBlock(
                    document,
                    "Correct Answer: " + safe(question.correctAnswer())
            );
        }

        if (question.questionType() == QuestionType.IDENTIFICATION) {
            addSimpleAnswerBlock(
                    document,
                    "Correct Answer: " + safe(question.correctAnswer())
            );
        }

        if (question.questionType() == QuestionType.ESSAY) {
            addEssayAnswerBlock(document, question, rubrics);
        }

        Paragraph spacer = new Paragraph("");
        spacer.setSpacingAfter(2f);
        document.add(spacer);
    }

    private void addMultipleChoiceAnswerBlock(
            Document document,
            List<ReportChoiceDTO> choices
    ) throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(5f);
        table.setWidths(new float[]{1f, 1f});

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);
        Font correctFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);

        String[] labels = {"A", "B", "C", "D"};

        for (int i = 0; i < 4; i++) {
            ReportChoiceDTO choice = i < choices.size() ? choices.get(i) : null;

            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(0f);
            cell.setPaddingBottom(0f);
            cell.setPaddingLeft(8f);
            cell.setPaddingRight(2f);

            if (choice == null) {
                cell.addElement(new Phrase(""));
                table.addCell(cell);
                continue;
            }

            boolean correct = Boolean.TRUE.equals(choice.correct());

            Phrase phrase = new Phrase();
            phrase.add(new Chunk(labels[i] + ". ", correct ? correctFont : normalFont));
            Chunk answerChunk = new Chunk(
                    safe(choice.choiceText()),
                    correct ? correctFont : normalFont
            );

            if (correct) {
                answerChunk.setBackground(new Color(220, 245, 220), 2f, 1f, 2f, 2f);
            }

            phrase.add(answerChunk);

            cell.addElement(phrase);
            cell.setMinimumHeight(10f);
            table.addCell(cell);
        }

        document.add(table);
    }

    private void addAnswerKeyQuestionRow(
            Document document,
            String questionText,
            String points
    ) throws DocumentException {

        Font questionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font pointsFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5f, 0.8f});
        table.setSpacingAfter(3f);

        PdfPCell questionCell = new PdfPCell(new Phrase(questionText, questionFont));
        questionCell.setBorder(Rectangle.NO_BORDER);
        questionCell.setPadding(0f);

        PdfPCell pointsCell = new PdfPCell(new Phrase(points, pointsFont));
        pointsCell.setBorder(Rectangle.NO_BORDER);
        pointsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        pointsCell.setPadding(0f);

        table.addCell(questionCell);
        table.addCell(pointsCell);

        document.add(table);
    }

    private void addSimpleAnswerBlock(
            Document document,
            String text
    ) throws DocumentException {

        Font answerFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

        Paragraph paragraph = new Paragraph();
        paragraph.setIndentationLeft(16f);
        paragraph.setSpacingAfter(6f);
        paragraph.setLeading(12f);

        Chunk chunk = new Chunk(text, answerFont);

        chunk.setBackground(new Color(220, 245, 220), 2f, 1f, 2f, 2f);

        paragraph.add(chunk);

        document.add(paragraph);
    }

    private void addEssayAnswerBlock(
            Document document,
            ReportQuestionDTO question,
            List<ReportRubricDTO> rubrics
    ) throws DocumentException {

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);

        Paragraph instructionLabel = new Paragraph("Instructions:", labelFont);
        instructionLabel.setIndentationLeft(16f);
        document.add(instructionLabel);

        Paragraph instruction = new Paragraph(
                safe(question.questionInstruction()).isBlank()
                        ? "No instructions provided."
                        : safe(question.questionInstruction()),
                normalFont
        );
        instruction.setIndentationLeft(28f);
        instruction.setSpacingAfter(3f);
        document.add(instruction);

        Paragraph rubricLabel = new Paragraph("Rubrics:", labelFont);
        rubricLabel.setIndentationLeft(16f);
        document.add(rubricLabel);

        if (rubrics == null || rubrics.isEmpty()) {
            Paragraph none = new Paragraph("No rubrics provided.", normalFont);
            none.setIndentationLeft(28f);
            document.add(none);
            return;
        }

        for (ReportRubricDTO rubric : rubrics) {
            Paragraph item = new Paragraph(
                    "• " + safe(rubric.criteriaName()) + " - " + rubric.percentage() + "%",
                    normalFont
            );
            item.setIndentationLeft(28f);
            item.setSpacingAfter(1f);
            document.add(item);
        }
    }

    // STUDENT ANSWER FIRST PAGE
    private void addStudentAnswerSectionCover(
            Document document,
            PdfWriter writer
    ) throws DocumentException {

        document.newPage();

        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = document.getPageSize();

        float centerY = page.getHeight() / 2f;

        Font titleFont =
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);

        Paragraph title = new Paragraph(
                "STUDENT ANSWER SHEETS",
                titleFont
        );

        title.setAlignment(Element.ALIGN_CENTER);

        ColumnText column = new ColumnText(canvas);

        column.setSimpleColumn(
                72f,
                centerY - 40f,
                page.getWidth() - 72f,
                centerY + 40f
        );

        column.addElement(title);
        column.go();
    }

    // STUDENT SUMMARY
    private void addStudentSummary(
            Document document,
            Long examId,
            String classOfferingId,
            BigDecimal totalPoints,
            Integer timeLimitMinutes
    ) throws DocumentException {

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

        Map<String, String> violationsByStudentId =
                violations.stream()
                        .collect(Collectors.groupingBy(
                                ReportStudentViolationDTO::studentId,
                                Collectors.mapping(
                                        ReportStudentViolationDTO::violationType,
                                        Collectors.joining(", ")
                                )
                        ));

        List<ReportStudentDurationDTO> durations =
                reportRepository.findStudentDurationsForReport(
                        examId,
                        classOfferingId
                );

        Map<Long, Long> durationMsByAttemptId =
                durations.stream()
                        .collect(Collectors.toMap(
                                ReportStudentDurationDTO::attemptId,
                                ReportStudentDurationDTO::totalDurationMs
                        ));

        document.newPage();

        Paragraph title = centered(
                "STUDENT SUMMARY",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)
        );
        title.setSpacingAfter(10f);
        document.add(title);

        addStudentSummaryOverview(
                document,
                students,
                violationsByStudentId,
                totalPoints
        );

        addStudentSummaryTable(
                document,
                students,
                violationsByStudentId,
                totalPoints,
                timeLimitMinutes,
                durationMsByAttemptId
        );
    }

    private void addStudentSummaryOverview(
            Document document,
            List<ReportStudentSummaryDTO> students,
            Map<String, String> violationsByStudentId,
            BigDecimal totalPoints
    ) throws DocumentException {

        long totalStudents = students.size();

        long submitted = students.stream()
                .filter(s -> "SUBMITTED".equalsIgnoreCase(s.attemptStatus()))
                .count();

        long autoSubmitted = students.stream()
                .filter(s -> "AUTO_SUBMITTED".equalsIgnoreCase(s.attemptStatus()))
                .count();

        long didNotTake = students.stream()
                .filter(s -> "DID_NOT_TAKE".equalsIgnoreCase(s.attemptStatus()))
                .count();

        long withViolations = violationsByStudentId.size();

        List<ReportStudentSummaryDTO> takers = students.stream()
                .filter(s -> !"DID_NOT_TAKE".equalsIgnoreCase(s.attemptStatus()))
                .toList();

        ReportStudentSummaryDTO highest = takers.stream()
                .max(Comparator.comparingDouble(s -> s.scorePercentage() == null ? 0.0 : s.scorePercentage()))
                .orElse(null);

        ReportStudentSummaryDTO lowest = takers.stream()
                .min(Comparator.comparingDouble(s -> s.scorePercentage() == null ? 0.0 : s.scorePercentage()))
                .orElse(null);

        double averagePercentage = takers.stream()
                .map(ReportStudentSummaryDTO::scorePercentage)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double averageScore = takers.stream()
                .map(ReportStudentSummaryDTO::totalScore)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);

        PdfPTable overviewTable = new PdfPTable(2);
        overviewTable.setWidthPercentage(100);
        overviewTable.setWidths(new float[]{1f, 1f});
        overviewTable.setSpacingAfter(10f);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(4f);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(4f);

        PdfPTable left = new PdfPTable(2);
        left.setWidthPercentage(100);
        left.setWidths(new float[]{1.2f, 1f});

        addSummaryMetric(left, "Total Students:", String.valueOf(totalStudents), labelFont, valueFont);
        addSummaryMetric(left, "Submitted:", formatCountWithPercentage(submitted, totalStudents), labelFont, valueFont);
        addSummaryMetric(left, "Auto Submitted:", formatCountWithPercentage(autoSubmitted, totalStudents), labelFont, valueFont);
        addSummaryMetric(left, "Did Not Take:", formatCountWithPercentage(didNotTake, totalStudents), labelFont, valueFont);

        PdfPTable right = new PdfPTable(2);
        right.setWidthPercentage(100);
        right.setWidths(new float[]{1.2f, 1f});

        addSummaryMetric(right, "With Violations:", formatCountWithPercentage(withViolations, totalStudents), labelFont, valueFont);
        addSummaryMetric(right, "Highest Score:", formatStudentScore(highest, totalPoints), labelFont, valueFont);
        addSummaryMetric(right, "Average Score:", formatScoreAverage(averageScore, averagePercentage, totalPoints), labelFont, valueFont);
        addSummaryMetric(right, "Lowest Score:", formatStudentScore(lowest, totalPoints), labelFont, valueFont);

        leftCell.addElement(left);
        rightCell.addElement(right);

        overviewTable.addCell(leftCell);
        overviewTable.addCell(rightCell);

        document.add(overviewTable);
    }

    private void addSummaryMetric(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPaddingBottom(3f);
        labelCell.setPaddingRight(4f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setPaddingBottom(3f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addStudentSummaryTable(
            Document document,
            List<ReportStudentSummaryDTO> students,
            Map<String, String> violationsByStudentId,
            BigDecimal totalPoints,
            Integer timeLimitMinutes,
            Map<Long, Long> durationMsByAttemptId
    ) throws DocumentException {

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

    // STUDENT ANSWER

    private void addStudentAnswerPages(
            Document document,
            Long examId,
            String classOfferingId,
            ReportExamHeaderDTO header
    ) throws DocumentException {

        List<ReportStudentSummaryDTO> students =
                reportRepository.findStudentSummariesForReport(examId, classOfferingId);

        List<ReportStudentAnswerDTO> answers =
                reportRepository.findStudentAnswersForReport( examId, classOfferingId);

        List<ReportEssayRubricScoreDTO> rubricScores =
                reportRepository.findEssayRubricScoresForReport( examId, classOfferingId);

        Map<String, List<ReportEssayRubricScoreDTO>> rubricScoresByAttemptAndQuestion =
                rubricScores.stream().collect(Collectors.groupingBy(
                        r -> r.attemptId() + "-" + r.questionId()
                        ));

        List<ReportChoiceDTO> choices =
                reportRepository.findChoicesForReport(examId);

        Map<Long, List<ReportChoiceDTO>> choicesByQuestionId =
                choices.stream().collect(Collectors.groupingBy(ReportChoiceDTO::questionId));

        Map<String, List<ReportStudentAnswerDTO>> answersByStudentId =
                answers.stream().collect(Collectors.groupingBy(ReportStudentAnswerDTO::studentId));

        List<Long> answerIds =
                answers.stream()
                        .map(ReportStudentAnswerDTO::answerId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        Map<Long, List<ExamAnswerReviewLog>> feedbackLogsByAnswerId =
                answerIds.isEmpty()
                        ? Map.of()
                        : examAnswerReviewLogRepository.findFeedbackLogsByAnswerIds(answerIds)
                            .stream()
                            .collect(Collectors.groupingBy(
                                    log -> log.getAnswer().getAnswerId()
                            ));

        for (ReportStudentSummaryDTO student : students) {
            if ("DID_NOT_TAKE".equalsIgnoreCase(student.attemptStatus())) {
                continue;
            }

            document.newPage();

            List<ReportStudentAnswerDTO> studentAnswers = answersByStudentId.getOrDefault(student.studentId(), List.of());

            studentAnswerPdfRenderer.renderStudentAnswer(
                    document,
                    header,
                    student,
                    studentAnswers,
                    choicesByQuestionId,
                    rubricScoresByAttemptAndQuestion,
                    feedbackLogsByAnswerId
            );
        }
    }

    // HELPER

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

    private String formatPoints(BigDecimal points) {
        if (points == null) return "0";
        return points.stripTrailingZeros().toPlainString();
    }

    private String formatSchedule(OffsetDateTime start, OffsetDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

        if (start == null || end == null) {
            return "";
        }

        return start.format(formatter) + " - " + end.format(formatter);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatViolationDisplay(String violationText) {
        if (violationText == null || violationText.isBlank() || "None".equalsIgnoreCase(violationText)) {
            return "None";
        }

        String[] parts = violationText.split(",");

        if (parts.length == 1) {
            return parts[0].trim().replace("_", " ");
        }

        return "Multiple Violations";
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

    private String formatPercentage(double value) {
        if (value % 1 == 0) {
            return ((int) value) + "%";
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

    private String formatCountWithPercentage(long count, long total) {
        if (total <= 0) {
            return count + " (0%)";
        }

        double percentage = ((double) count / total) * 100;

        return count + " (" + formatPercentage(percentage) + ")";
    }

    private String formatStudentScore(
            ReportStudentSummaryDTO student,
            BigDecimal totalPoints
    ) {
        if (student == null) {
            return "-";
        }

        return formatPoints(student.totalScore())
                + "/"
                + formatPoints(totalPoints)
                + " ("
                + formatPercentage(student.scorePercentage())
                + ")";
    }

    private String formatScoreAverage(
            double averageScore,
            double averagePercentage,
            BigDecimal totalPoints
    ) {
        return formatPoints(averageScore)
                + "/"
                + formatPoints(totalPoints)
                + " ("
                + formatPercentage(averagePercentage)
                + ")";
    }

    private String formatDurationFromMs(
            ReportStudentSummaryDTO student,
            Map<Long, Long> durationMsByAttemptId,
            Integer timeLimitMinutes
    ) {
        if (student.attemptId() == null) {
            return "-";
        }

        long totalMs = durationMsByAttemptId.getOrDefault(student.attemptId(), 0L);

        if (totalMs <= 0) {
            return "-";
        }

        long totalSeconds = totalMs / 1000;

        long maxSeconds = timeLimitMinutes == null || timeLimitMinutes <= 0
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


    @Override
    public boolean supports(String reportType) {
        return "EXAM_PORTFOLIO".equalsIgnoreCase(reportType);
    }
}