package com.example.backend.dto.exam.response;

import java.time.OffsetDateTime;

public interface ExamListProjection {
    Long getExamId();
    String getTitle();
    String getDescription();
    String getDateCreated();
    String getValidUntil();
    String getStatus();
    Integer getDurationMinutes();
    String getAssigned();
    Long getTotalTakers();
    Long getSubmittedTakers();
    OffsetDateTime getStartDateTime();
    OffsetDateTime getEndDateTime();
    String getExamMode();
    String getCreatedBy();
    String getUpdatedBy();
}