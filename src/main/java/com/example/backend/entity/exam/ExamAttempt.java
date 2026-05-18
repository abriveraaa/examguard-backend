package com.example.backend.entity.exam;

import com.example.backend.entity.enums.ExamAttemptStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "exam_attempt",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_exam_attempt_student",
                        columnNames = {"exam_id", "student_id"}
                )
        }
)
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExamAttemptStatus status = ExamAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "question_order", columnDefinition = "TEXT", nullable = false)
    private String questionOrder;

    @Column(name = "score_percentage")
    private Double scorePercentage;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "review_status")
    private String reviewStatus = "PENDING_REVIEW";

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "violation_feedback", columnDefinition = "TEXT")
    private String violationFeedback;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();


    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getAttemptId() { return attemptId; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public ExamAttemptStatus getStatus() { return status; }
    public void setStatus(ExamAttemptStatus status) { this.status = status; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(String questionOrder) { this.questionOrder = questionOrder; }

    public Double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(Double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
