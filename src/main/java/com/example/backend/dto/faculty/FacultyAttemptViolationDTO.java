package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class FacultyAttemptViolationDTO {

    private Long violationId;
    private Long questionId;
    private String violationType;
    private String severity;
    private String violationMessage;
    private String evidenceUrl;
    private Integer attemptNumber;
    private OffsetDateTime occurredAt;

    private String reviewStatus;
    private String reviewedBy;
    private OffsetDateTime reviewedAt;

    public FacultyAttemptViolationDTO(
            Long violationId,
            Long questionId,
            String violationType,
            String severity,
            String violationMessage,
            String evidenceUrl,
            Integer attemptNumber,
            OffsetDateTime occurredAt,
            String reviewStatus,
            String reviewedBy,
            OffsetDateTime reviewedAt
    ) {
        this.violationId = violationId;
        this.questionId = questionId;
        this.violationType = violationType;
        this.severity = severity;
        this.violationMessage = violationMessage;
        this.evidenceUrl = evidenceUrl;
        this.attemptNumber = attemptNumber;
        this.occurredAt = occurredAt;

        this.reviewStatus = reviewStatus;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
    }
}
