package com.example.backend.service.exam;

import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.exam.request.ChoiceRequest;
import com.example.backend.dto.exam.request.EssayRubricRequest;
import com.example.backend.dto.exam.request.QuestionRequest;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExamTemplateService {

    @TrackActivity(
            module = "EXAM_TEMPLATE",
            action = "PARSE_TEMPLATE",
            message = "Exam template parsing attempted"
    )
    public List<QuestionRequest> parseTemplate(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new RuntimeException("Only .xlsx files are allowed.");
        }

        List<QuestionRequest> questions = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String type = getCellValue(row, 0);
                String questionText = getCellValue(row, 1);
                String choiceA = getCellValue(row, 2);
                String choiceB = getCellValue(row, 3);
                String choiceC = getCellValue(row, 4);
                String choiceD = getCellValue(row, 5);
                String correctAnswer = getCellValue(row, 6);
                String pointsText = getCellValue(row, 7);
                String rubricText = getCellValue(row, 8);
                String questionInstruction = getCellValue(row, 9);

                if (isBlank(type) && isBlank(questionText)) {
                    continue;
                }

                if (isBlank(type)) {
                    throw new RuntimeException("Row " + (rowIndex + 1) + ": Question type is required.");
                }

                if (isBlank(questionText)) {
                    throw new RuntimeException("Row " + (rowIndex + 1) + ": Question text is required.");
                }

                QuestionRequest question = new QuestionRequest();
                question.setQuestionType(type.trim().toUpperCase());

                List<String> supportedTypes = List.of(
                        "MULTIPLE_CHOICE",
                        "TRUE_FALSE",
                        "IDENTIFICATION",
                        "ESSAY"
                );

                if (!supportedTypes.contains(question.getQuestionType())) {
                    throw new RuntimeException(
                            "Row " + (rowIndex + 1) +
                                    ": Invalid question type: " + type
                    );
                }

                question.setQuestionText(questionText.trim());
                question.setPoints(parsePoints(pointsText, rowIndex));
                question.setQuestionInstruction(questionInstruction);

                if (!"ESSAY".equals(question.getQuestionType()) && !isBlank(rubricText)) {
                    throw new RuntimeException("Row " + (rowIndex + 1) + ": Rubric is only allowed for ESSAY questions.");
                }

                switch (question.getQuestionType()) {
                    case "MULTIPLE_CHOICE" -> {
                        validateMultipleChoice(rowIndex, choiceA, choiceB, choiceC, choiceD, correctAnswer);

                        List<ChoiceRequest> choices = new ArrayList<>();

                        boolean isA = isCorrectChoice(correctAnswer, "A", choiceA);
                        boolean isB = isCorrectChoice(correctAnswer, "B", choiceB);
                        boolean isC = isCorrectChoice(correctAnswer, "C", choiceC);
                        boolean isD = isCorrectChoice(correctAnswer, "D", choiceD);

                        int correctCount = (isA ? 1 : 0) + (isB ? 1 : 0) + (isC ? 1 : 0) + (isD ? 1 : 0);

                        if (correctCount != 1) {
                            throw new RuntimeException(
                                    "Row " + (rowIndex + 1) + ": Exactly one correct answer must be defined."
                            );
                        }

                        choices.add(createChoice("A", choiceA, 0, isA));
                        choices.add(createChoice("B", choiceB, 1, isB));
                        choices.add(createChoice("C", choiceC, 2, isC));
                        choices.add(createChoice("D", choiceD, 3, isD));

                        question.setChoices(choices);
                    }

                    case "TRUE_FALSE" -> {
                        if (!correctAnswer.equalsIgnoreCase("TRUE") &&
                                !correctAnswer.equalsIgnoreCase("FALSE")) {
                            throw new RuntimeException("Row " + (rowIndex + 1) + ": Correct answer must be TRUE or FALSE.");
                        }

                        question.setCorrectAnswer(correctAnswer.toUpperCase());
                    }

                    case "IDENTIFICATION" -> {

                        if (isBlank(correctAnswer)) {
                            throw new RuntimeException(
                                    "Row " + (rowIndex + 1) + ": Correct answer is required."
                            );
                        }

                        String normalizedAnswers = normalizeAcceptedAnswers(correctAnswer);

                        question.setCorrectAnswer(normalizedAnswers);
                    }

                    case "ESSAY" -> {
                        if (!isBlank(correctAnswer)) {
                            throw new RuntimeException("Row " + (rowIndex + 1) + ": ESSAY questions should not have CorrectAnswer.");
                        }

                        question.setCorrectAnswer("");
                        question.setRubrics(parseRubric(rubricText, rowIndex));
                        if (!isBlank(rubricText) && question.getRubrics().isEmpty()) {
                            throw new RuntimeException(
                                    "Row " + (rowIndex + 1) + ": Essay rubric is invalid."
                            );
                        }
                    }
                }

                questions.add(question);
            }
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No valid questions found in the template.");
        }

        return questions;
    }

    private List<EssayRubricRequest> parseRubric(String rubricText, int rowIndex) {
        List<EssayRubricRequest> rubrics = new ArrayList<>();

        if (isBlank(rubricText)) {
            return rubrics;
        }

        String[] parts = rubricText.split(";");

        int order = 1;
        BigDecimal total = BigDecimal.ZERO;

        for (String part : parts) {
            if (isBlank(part)) continue;

            String[] pair = part.trim().split("=");

            if (pair.length != 2) {
                throw new RuntimeException(
                        "Row " + (rowIndex + 1) + ": Invalid rubric format. Use Grammar=20; Content=40"
                );
            }

            String criterionName = pair[0].trim();
            String weightText = pair[1].trim();

            if (isBlank(criterionName)) {
                throw new RuntimeException("Row " + (rowIndex + 1) + ": Rubric criterion name cannot be blank.");
            }

            BigDecimal weight;

            try {
                weight = new BigDecimal(weightText);
            } catch (Exception e) {
                throw new RuntimeException("Row " + (rowIndex + 1) + ": Rubric weight must be a number.");
            }

            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Row " + (rowIndex + 1) + ": Rubric weight must be greater than 0.");
            }

            total = total.add(weight);

            EssayRubricRequest rubric = new EssayRubricRequest();
            rubric.setCriterionName(criterionName);
            rubric.setWeightPercentage(weight);
            rubric.setDescription("");
            rubric.setDisplayOrder(order++);

            rubrics.add(rubric);
        }

        if (total.compareTo(new BigDecimal("100")) != 0) {
            throw new RuntimeException(
                    "Row " + (rowIndex + 1) + ": Essay rubric total must be exactly 100%. Current total: " + total + "%"
            );
        }

        return rubrics;
    }

    private ChoiceRequest createChoice(String label, String text, int order, boolean isCorrect) {
        ChoiceRequest choice = new ChoiceRequest();

        choice.setChoiceLabel(label);
        choice.setChoiceText(text);
        choice.setChoiceOrder(order);
        choice.setCorrect(isCorrect);

        return choice;
    }

    private void validateMultipleChoice(
            int rowIndex,
            String choiceA,
            String choiceB,
            String choiceC,
            String choiceD,
            String correctAnswer
    ) {
        if (isBlank(choiceA) || isBlank(choiceB) || isBlank(choiceC) || isBlank(choiceD)) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": All 4 choices are required for multiple choice.");
        }

        if (isBlank(correctAnswer)) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": Correct answer is required.");
        }

        boolean validLetter = correctAnswer.equalsIgnoreCase("A")
                || correctAnswer.equalsIgnoreCase("B")
                || correctAnswer.equalsIgnoreCase("C")
                || correctAnswer.equalsIgnoreCase("D");

        boolean matchesChoiceText = correctAnswer.equalsIgnoreCase(choiceA)
                || correctAnswer.equalsIgnoreCase(choiceB)
                || correctAnswer.equalsIgnoreCase(choiceC)
                || correctAnswer.equalsIgnoreCase(choiceD);

        if (!validLetter && !matchesChoiceText) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": Correct answer must be A, B, C, D, or exact choice text.");
        }
    }

    private boolean isCorrectChoice(String correctAnswer, String letter, String choiceText) {
        if (isBlank(correctAnswer)) return false;

        return correctAnswer.equalsIgnoreCase(letter)
                || correctAnswer.equalsIgnoreCase(choiceText);
    }

    private BigDecimal parsePoints(String pointsText, int rowIndex) {
        if (isBlank(pointsText)) {
            return BigDecimal.ONE;
        }

        try {
            BigDecimal points = new BigDecimal(pointsText.trim());

            if (points.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException();
            }

            return points;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Row " + (rowIndex + 1) + ": Points must be a positive number."
            );
        }
    }

    private String getCellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);

        if (cell == null) return "";

        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeAcceptedAnswers(String value) {

        String[] parts = value.split(";");

        List<String> cleaned = new ArrayList<>();

        for (String part : parts) {

            if (isBlank(part)) {
                continue;
            }

            cleaned.add(part.trim());
        }

        if (cleaned.isEmpty()) {
            return "";
        }

        return String.join(";", cleaned);
    }
}