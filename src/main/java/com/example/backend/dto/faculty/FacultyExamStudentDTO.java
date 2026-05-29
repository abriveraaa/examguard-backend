package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
public class FacultyExamStudentDTO implements Serializable {

    private String studentId;
    private String studentName;
    private String emailAddress;
    private String programCode;
    private String attemptStatus;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private Double scorePercentage;
    private Long violationCount;
    private Boolean needsChecking;
    private String reviewStatus;

    public FacultyExamStudentDTO(
            String studentId,
            String studentName,
            String emailAddress,
            String programCode,
            String attemptStatus,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            Double scorePercentage,
            Long violationCount,
            Boolean needsChecking,
            String reviewStatus
    ) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.emailAddress = emailAddress;
        this.programCode = programCode;
        this.attemptStatus = attemptStatus;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.scorePercentage = scorePercentage;
        this.violationCount = violationCount;
        this.needsChecking = needsChecking;
        this.reviewStatus = reviewStatus;
    }

    // getters and setters
}
