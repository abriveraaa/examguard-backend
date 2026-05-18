package com.example.backend.dto.student.result;

import lombok.Getter;

import java.util.List;

@Getter
public class StudentExamResultQuestionResponse {

    private Long questionId;
    private Integer questionNumber;
    private String questionType;

    private String questionText;
    private String questionImageUrl;

    private Double points;
    private Double earnedPoints;

    private String studentAnswer;
    private String correctAnswer;

    private Boolean correct;
    private String feedback;

    private List<StudentExamResultChoiceResponse> choices;
    private List<StudentExamResultViolationResponse> violations;
    private List<StudentExamResultRubricResponse> rubrics;

    public StudentExamResultQuestionResponse(
            Long questionId,
            Integer questionNumber,
            String questionType,
            String questionText,
            String questionImageUrl,
            Double points,
            Double earnedPoints,
            String studentAnswer,
            String correctAnswer,
            Boolean correct,
            String feedback,
            List<StudentExamResultChoiceResponse> choices,
            List<StudentExamResultViolationResponse> violations,
            List<StudentExamResultRubricResponse> rubrics
    ) {
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionType = questionType;
        this.questionText = questionText;
        this.questionImageUrl = questionImageUrl;
        this.points = points;
        this.earnedPoints = earnedPoints;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
        this.feedback = feedback;
        this.choices = choices;
        this.violations = violations;
        this.rubrics = rubrics;
    }
}
