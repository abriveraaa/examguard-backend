package com.example.backend.entity.exam;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "exam_answer_review_log")
public class ExamAnswerReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private ExamAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ExamQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id")
    private ExamViolationLog violation;

    @Column(length = 50, nullable = false)
    private String actionType;

    @Column(columnDefinition = "TEXT")
    private String previousValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal scoreBefore;

    @Column(precision = 10, scale = 2)
    private BigDecimal scoreAfter;

    @Column(precision = 10, scale = 2)
    private BigDecimal deduction;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 50)
    private String createdBy;

    @Column(nullable = false, length = 20)
    private String createdByRole;

    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}