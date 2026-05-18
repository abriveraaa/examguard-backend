package com.example.backend.report.exam.renderer;

import com.example.backend.entity.enums.QuestionType;
import com.example.backend.report.exam.dto.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class StudentAnswerPdfRenderer {

    private final Font unicodeFont;

    public StudentAnswerPdfRenderer() {
        try {
            BaseFont unicodeBaseFont = BaseFont.createFont(
                    Objects.requireNonNull(getClass().getResource("/fonts/NotoSans-Regular.ttf")).toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
            this.unicodeFont = new Font(unicodeBaseFont, 8.5f, Font.BOLD);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Unicode font.", e);
        }
    }

    public void renderStudentAnswer(
            Document document,
            ReportExamHeaderDTO header,
            ReportStudentSummaryDTO student,
            List<ReportStudentAnswerDTO> studentAnswers,
            Map<Long, List<ReportChoiceDTO>> choicesByQuestionId,
            Map<String, List<ReportEssayRubricScoreDTO>> rubricScoresByAttemptAndQuestion
    ) throws DocumentException {

        addCourseAndExamTitle(document, header);

        addStudentAnswerHeader(
                document,
                student.studentName(),
                student.studentId(),
                formatStudentScore(student, header.totalPoints()),
                formatSubmittedAt(student.submittedAt()),
                student.programCode(),
                student.faculty()
        );

        addStudentAnswerGroup(document, "I. MULTIPLE CHOICE", QuestionType.MULTIPLE_CHOICE, studentAnswers, choicesByQuestionId, rubricScoresByAttemptAndQuestion);
        addStudentAnswerGroup(document, "II. TRUE OR FALSE", QuestionType.TRUE_FALSE, studentAnswers, choicesByQuestionId, rubricScoresByAttemptAndQuestion);
        addStudentAnswerGroup(document, "III. IDENTIFICATION", QuestionType.IDENTIFICATION, studentAnswers, choicesByQuestionId, rubricScoresByAttemptAndQuestion);
        addStudentAnswerGroup(document, "IV. ESSAY", QuestionType.ESSAY, studentAnswers, choicesByQuestionId, rubricScoresByAttemptAndQuestion);
    }

    private void addCourseAndExamTitle(Document document, ReportExamHeaderDTO header) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        Paragraph courseLine = new Paragraph(
                safe(header.courseCode()).toUpperCase() + ": " + safe(header.courseDescription()).toUpperCase(),
                titleFont
        );
        courseLine.setAlignment(Element.ALIGN_CENTER);
        courseLine.setSpacingAfter(2f);
        document.add(courseLine);

        Paragraph examTitle = new Paragraph(safe(header.examTitle()).toUpperCase(), titleFont);
        examTitle.setAlignment(Element.ALIGN_CENTER);
        examTitle.setSpacingAfter(10f);
        document.add(examTitle);
    }

    private void addStudentAnswerHeader(
            Document document,
            String studentName,
            String studentId,
            String totalScore,
            String submittedAt,
            String programCode,
            String faculty
    ) throws DocumentException {

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});
        table.setSpacingAfter(10f);

        addStudentHeaderCell(table, "Student Name:", studentName, labelFont, valueFont);
        addStudentHeaderCell(table, "Total Score:", totalScore, labelFont, valueFont);
        addStudentHeaderCell(table, "Student Number:", studentId, labelFont, valueFont);
        addStudentHeaderCell(table, "Submitted At:", submittedAt, labelFont, valueFont);
        addStudentHeaderCell(table, "Program:", programCode, labelFont, valueFont);
        addStudentHeaderCell(table, "Faculty:", faculty, labelFont, valueFont);

        document.add(table);
    }

    private void addStudentHeaderCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label + " ", labelFont));
        phrase.add(new Chunk(safe(value), valueFont));

        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4f);
        table.addCell(cell);
    }

    private void addStudentAnswerGroup(
            Document document,
            String sectionTitle,
            QuestionType questionType,
            List<ReportStudentAnswerDTO> answers,
            Map<Long, List<ReportChoiceDTO>> choicesByQuestionId,
            Map<String, List<ReportEssayRubricScoreDTO>> rubricScoresByAttemptAndQuestion
    ) throws DocumentException {

        List<ReportStudentAnswerDTO> filtered =
                answers.stream()
                        .filter(a -> a.questionType() == questionType)
                        .toList();

        if (filtered.isEmpty()) return;

        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        Paragraph section = new Paragraph(sectionTitle, sectionFont);
        section.setSpacingBefore(6f);
        section.setSpacingAfter(4f);
        document.add(section);

        int number = 1;

        for (ReportStudentAnswerDTO answer : filtered) {
            addStudentAnswerBlock(
                    document,
                    number++,
                    answer,
                    choicesByQuestionId.getOrDefault(answer.questionId(), List.of()),
                    rubricScoresByAttemptAndQuestion.getOrDefault(
                            answer.attemptId() + "-" + answer.questionId(),
                            List.of()
                    )
            );
        }
    }

    private void addStudentAnswerBlock(
            Document document,
            int number,
            ReportStudentAnswerDTO answer,
            List<ReportChoiceDTO> choices,
            List<ReportEssayRubricScoreDTO> rubricScores
    ) throws DocumentException {

        addStudentQuestionRow(
                document,
                number + ". " + safe(answer.questionText()),
                formatPoints(answer.earnedPoints()) + "/" + formatPoints(answer.questionPoints())
        );

        if (answer.questionType() == QuestionType.MULTIPLE_CHOICE) {
            addStudentMultipleChoiceBlock(document, choices, answer.selectedChoiceId());

        } else if (answer.questionType() == QuestionType.TRUE_FALSE ||
                answer.questionType() == QuestionType.IDENTIFICATION) {

            addStudentSimpleAnswer(document, "Student Answer: " + safe(answer.answerText()), answer.isCorrect());
            addPlainAnswerLine(document, "Correct Answer: " + safe(answer.correctAnswer()));

        } else if (answer.questionType() == QuestionType.ESSAY) {

            addPlainAnswerLine(document, "Instructions: " + safe(answer.questionInstruction()));
            addStudentSimpleAnswer(document, "Student Answer: " + safe(answer.answerText()), null);
            addEssayStudentScoresOnly(document, rubricScores);
            addPlainAnswerLine(
                    document,
                    "Total Score Achieved: "
                            + formatPoints(answer.earnedPoints())
                            + "/"
                            + formatPoints(answer.questionPoints())
            );
        }

        addGeneralFeedbackLine(document, answer.facultyFeedback());

        Paragraph spacer = new Paragraph("");
        spacer.setSpacingAfter(3f);
        document.add(spacer);
    }

    private void addStudentQuestionRow(Document document, String questionText, String points) throws DocumentException {
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

    private void addStudentMultipleChoiceBlock(
            Document document,
            List<ReportChoiceDTO> choices,
            Long selectedChoiceId
    ) throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(5f);
        table.setWidths(new float[]{1f, 1f});

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);

        String[] labels = {"A", "B", "C", "D"};

        for (int i = 0; i < 4; i++) {
            ReportChoiceDTO choice = i < choices.size() ? choices.get(i) : null;

            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(0f);
            cell.setPaddingBottom(1.5f);
            cell.setPaddingLeft(8f);
            cell.setPaddingRight(2f);

            if (choice == null) {
                table.addCell(cell);
                continue;
            }

            boolean selected = selectedChoiceId != null && selectedChoiceId.equals(choice.choiceId());
            boolean correct = Boolean.TRUE.equals(choice.correct());

            Phrase phrase = new Phrase();
            phrase.add(new Chunk(labels[i] + ". ", selected ? boldFont : normalFont));

            Chunk answerChunk = new Chunk(safe(choice.choiceText()), selected ? boldFont : normalFont);

            if (selected && correct) {
                answerChunk.setBackground(new Color(220, 245, 220), 2f, 1f, 2f, 2f);
            } else if (selected) {
                answerChunk.setBackground(new Color(255, 225, 225), 2f, 1f, 2f, 2f);
            } else if (correct) {
                answerChunk.setBackground(new Color(220, 245, 220), 2f, 1f, 2f, 2f);
            }

            phrase.add(answerChunk);

            if (selected && correct) {
                phrase.add(new Chunk("  ✓", unicodeFont));
            } else if (selected) {
                phrase.add(new Chunk("  ✕", unicodeFont));
            } else if (correct) {
                phrase.add(new Chunk("  ✓", unicodeFont));
            }

            cell.addElement(phrase);
            table.addCell(cell);
        }

        document.add(table);
    }

    private void addStudentSimpleAnswer(Document document, String text, Boolean correct) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8.8f);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.8f);

        Paragraph paragraph = new Paragraph();
        paragraph.setIndentationLeft(16f);
        paragraph.setSpacingAfter(3f);
        paragraph.setLeading(11f);

        Chunk chunk = new Chunk(text, Boolean.TRUE.equals(correct) ? boldFont : font);

        if (Boolean.TRUE.equals(correct)) {
            chunk.setBackground(new Color(220, 245, 220), 2f, 1f, 2f, 2f);
            paragraph.add(chunk);
            paragraph.add(new Chunk("  ✓", unicodeFont));
        } else if (Boolean.FALSE.equals(correct)) {
            chunk.setBackground(new Color(255, 225, 225), 2f, 1f, 2f, 2f);
            paragraph.add(chunk);
            paragraph.add(new Chunk("  ✕", unicodeFont));
        } else {
            paragraph.add(chunk);
        }

        document.add(paragraph);
    }

    private void addEssayStudentScoresOnly(
            Document document,
            List<ReportEssayRubricScoreDTO> rubricScores
    ) throws DocumentException {

        if (rubricScores == null || rubricScores.isEmpty()) {
            addPlainAnswerLine(document, "Student Rubric Score: No rubric scores available.");
            return;
        }

        addPlainAnswerLine(document, "Student Rubric Score:");

        for (ReportEssayRubricScoreDTO r : rubricScores) {
            addTwoColumnLine(
                    document,
                    safe(r.criterionName()),
                    formatPointsNoDecimal(r.scoreAwarded())
                            + "/"
                            + formatPointsNoDecimal(computeRubricPoints(r.questionPoints(), r.weightPercentage()))
                            + " points | "
                            + formatPercentageNoDecimal(r.scorePercentage())
                            + " attained"
                            + (safe(r.feedback()).isBlank() ? "" : " - " + safe(r.feedback()))
            );
        }
    }

    private void addTwoColumnLine(Document document, String label, String value) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(92);
        table.setWidths(new float[]{0.55f, 3.2f});
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setSpacingAfter(1f);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(0f);
        labelCell.setPaddingRight(3f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(0f);

        table.addCell(labelCell);
        table.addCell(valueCell);
        document.add(table);
    }

    private void addGeneralFeedbackLine(Document document, String feedback) throws DocumentException {
        if (feedback == null || feedback.isBlank()) return;

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.8f);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8.8f);

        Paragraph paragraph = new Paragraph();
        paragraph.setIndentationLeft(16f);
        paragraph.setSpacingBefore(2f);
        paragraph.setSpacingAfter(4f);
        paragraph.setLeading(11f);

        paragraph.add(new Chunk("Feedback: \n", labelFont));
        paragraph.add(new Chunk(feedback.trim(), valueFont));

        document.add(paragraph);
    }

    private void addPlainAnswerLine(Document document, String text) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8.8f);

        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setIndentationLeft(16f);
        paragraph.setSpacingAfter(3f);
        paragraph.setLeading(11f);

        document.add(paragraph);
    }

    private double computeRubricPoints(Double questionPoints, Double weightPercentage) {
        if (questionPoints == null || weightPercentage == null) return 0;
        return questionPoints * (weightPercentage / 100.0);
    }

    private String formatStudentScore(ReportStudentSummaryDTO student, BigDecimal totalPoints) {
        if (student == null) return "-";

        return formatPoints(student.totalScore())
                + "/"
                + formatPoints(totalPoints)
                + " ("
                + formatPercentage(student.scorePercentage())
                + ")";
    }

    private String formatSubmittedAt(OffsetDateTime submittedAt) {
        if (submittedAt == null) return "-";
        return submittedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));
    }

    private String formatPoints(Double value) {
        if (value == null) return "0";
        if (value % 1 == 0) return String.valueOf(value.intValue());
        return String.format("%.2f", value);
    }

    private String formatPoints(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatPercentage(Double value) {
        if (value == null) return "0%";
        if (value % 1 == 0) return value.intValue() + "%";
        return String.format("%.2f%%", value);
    }

    private String formatPointsNoDecimal(Double value) {
        if (value == null) return "0";
        return String.valueOf(Math.round(value));
    }

    private String formatPointsNoDecimal(double value) {
        return String.valueOf(Math.round(value));
    }

    private String formatPercentageNoDecimal(Double value) {
        if (value == null) return "0%";
        return Math.round(value) + "%";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}