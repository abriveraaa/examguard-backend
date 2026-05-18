package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class FacultyStudentViolationDTO {

    private Long attemptId;
    private Long examId;

    private String studentId;
    private String studentName;

    private String courseCode;
    private String sectionName;

    private Long violationCount;

    private String violationLabel;

    private String highestSeverity;

    private OffsetDateTime latestViolationAt;

    private String reviewStatus;

    public FacultyStudentViolationDTO(
            Long attemptId,
            Long examId,
            String studentId,
            String studentName,
            String courseCode,
            String sectionName,
            Long violationCount,
            String violationLabel,
            String highestSeverity,
            OffsetDateTime latestViolationAt,
            String reviewStatus
    ) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseCode = courseCode;
        this.sectionName = sectionName;
        this.violationCount = violationCount;
        this.violationLabel = violationLabel;
        this.highestSeverity = highestSeverity;
        this.latestViolationAt = latestViolationAt;
        this.reviewStatus = reviewStatus;
    }

    // getters and setters
}