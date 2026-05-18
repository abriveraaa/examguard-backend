package com.example.backend.dto.faculty;

import com.example.backend.dto.exam.request.EssayRubricRequest;
import com.example.backend.dto.exam.response.EssayRubricScoreResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FacultyAttemptAnswerReviewDTO {

    private Long answerId;
    private Long questionId;
    private Integer questionNumber;
    private String questionType;
    private String questionText;
    private String studentAnswer;
    private String correctAnswer;
    private BigDecimal points;
    private BigDecimal earnedPoints;
    private Boolean correct;
    private Boolean needsManualCheck;
    private Boolean manuallyReviewed;

    private String questionInstruction;
    private String facultyFeedback;
    private String reviewStatus;
    private Boolean needsChecking;

    private List<EssayRubricRequest> rubrics = new ArrayList<>();
    private List<EssayRubricScoreResponse> rubricScores = new ArrayList<>();
    private List<FacultyAttemptViolationDTO> violations = new ArrayList<>();

    public FacultyAttemptAnswerReviewDTO(
            Long answerId,
            Long questionId,
            Integer questionNumber,
            String questionType,
            String questionText,
            String studentAnswer,
            String correctAnswer,
            BigDecimal points,
            BigDecimal earnedPoints,
            Boolean correct,
            Boolean needsManualCheck,
            String questionInstruction,
            String facultyFeedback,
            String reviewStatus,
            Boolean needsChecking
    ) {
        this.answerId = answerId;
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionType = questionType;
        this.questionText = questionText;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
        this.points = points;
        this.earnedPoints = earnedPoints;
        this.correct = correct;
        this.needsManualCheck = needsManualCheck;
        this.questionInstruction = questionInstruction;
        this.facultyFeedback = facultyFeedback;
        this.reviewStatus = reviewStatus;
        this.needsChecking = needsChecking;
        this.manuallyReviewed = "REVIEWED".equalsIgnoreCase(reviewStatus);
    }
}
