package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
public class FacultySubmissionSummaryDTO implements Serializable {

    private Long attemptId;
    private Long examId;

    private String examTitle;

    private String studentId;
    private String studentName;

    private String courseCode;
    private String sectionName;

    private String attemptStatus;

    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;

    private Double scorePercentage;

    private Long violationCount;

    private Boolean needsChecking;

    public FacultySubmissionSummaryDTO(
            Long attemptId,
            Long examId,
            String examTitle,
            String studentId,
            String studentName,
            String courseCode,
            String sectionName,
            String attemptStatus,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            Double scorePercentage,
            Long violationCount,
            Boolean needsChecking
    ) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseCode = courseCode;
        this.sectionName = sectionName;
        this.attemptStatus = attemptStatus;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.scorePercentage = scorePercentage;
        this.violationCount = violationCount;
        this.needsChecking = needsChecking;
    }

    // getters and setters
}
