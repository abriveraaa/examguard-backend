package com.example.backend.dto.student.result;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class StudentExamResultViolationResponse {

    private Long violationId;
    private String violationType;
    private String severity;
    private String message;

    private String reviewStatus;
    private String reviewNotes;

    private String evidenceUrl;
    private OffsetDateTime occurredAt;

    public StudentExamResultViolationResponse(
            Long violationId,
            String violationType,
            String severity,
            String message,
            String reviewStatus,
            String reviewNotes,
            String evidenceUrl,
            OffsetDateTime occurredAt
    ) {
        this.violationId = violationId;
        this.violationType = violationType;
        this.severity = severity;
        this.message = message;
        this.reviewStatus = reviewStatus;
        this.reviewNotes = reviewNotes;
        this.evidenceUrl = evidenceUrl;
        this.occurredAt = occurredAt;
    }
}
