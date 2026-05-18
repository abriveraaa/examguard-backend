package com.example.backend.entity.exam;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@Entity
@Table(name = "exam_violation_log")
public class ExamViolationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long violationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ExamQuestion question;

    @Column(name = "violation_type", nullable = false, length = 50)
    private String violationType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "violation_message", columnDefinition = "TEXT")
    private String violationMessage;

    @Column(name = "attempt_number")
    private Integer attemptNumber = 1;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(name = "review_status")
    private String reviewStatus = "PENDING_REVIEW";

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (occurredAt == null) {
            occurredAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (severity == null || severity.isBlank()) {
            severity = "MINOR";
        }

        if (attemptNumber == null) {
            attemptNumber = 1;
        }
    }
}
