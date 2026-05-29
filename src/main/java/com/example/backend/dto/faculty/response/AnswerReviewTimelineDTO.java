package com.example.backend.dto.faculty.response;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
public class AnswerReviewTimelineDTO implements Serializable {

    private final Long reviewLogId;
    private final String actionType;
    private final String previousValue;
    private final String newValue;
    private final BigDecimal scoreBefore;
    private final BigDecimal scoreAfter;
    private final BigDecimal deduction;
    private final String notes;
    private final String createdBy;
    private final String createdByRole;
    private final OffsetDateTime createdAt;

    public AnswerReviewTimelineDTO(
            Long reviewLogId,
            String actionType,
            String previousValue,
            String newValue,
            BigDecimal scoreBefore,
            BigDecimal scoreAfter,
            BigDecimal deduction,
            String notes,
            String createdBy,
            String createdByRole,
            OffsetDateTime createdAt
    ) {
        this.reviewLogId = reviewLogId;
        this.actionType = actionType;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.scoreBefore = scoreBefore;
        this.scoreAfter = scoreAfter;
        this.deduction = deduction;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdByRole = createdByRole;
        this.createdAt = createdAt;
    }
}