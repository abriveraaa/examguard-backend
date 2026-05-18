package com.example.backend.dto.student.result;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class StudentExamResultResponse {

    private Long examId;
    private Long attemptId;

    private String title;
    private String courseCode;
    private String courseDescription;
    private String faculty;

    private Integer durationMinutes;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;

    private Double totalScore;
    private Double totalPoints;
    private Double scorePercentage;

    private String attemptStatus;
    private String reviewStatus;
    private Boolean resultsReleased;

    private String term;
    private String academicYear;

    private List<StudentExamResultQuestionResponse> questions;

    public StudentExamResultResponse(
            Long examId,
            Long attemptId,
            String title,
            String courseCode,
            String courseDescription,
            String faculty,
            String term,
            String academicYear,
            Integer durationMinutes,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            Double totalScore,
            Double totalPoints,
            Double scorePercentage,
            String attemptStatus,
            String reviewStatus,
            Boolean resultsReleased,
            List<StudentExamResultQuestionResponse> questions
    ) {
        this.examId = examId;
        this.attemptId = attemptId;
        this.title = title;
        this.courseCode = courseCode;
        this.courseDescription = courseDescription;
        this.faculty = faculty;
        this.term = term;
        this.academicYear = academicYear;
        this.durationMinutes = durationMinutes;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.totalScore = totalScore;
        this.totalPoints = totalPoints;
        this.scorePercentage = scorePercentage;
        this.attemptStatus = attemptStatus;
        this.reviewStatus = reviewStatus;
        this.resultsReleased = resultsReleased;
        this.questions = questions;
    }
}
