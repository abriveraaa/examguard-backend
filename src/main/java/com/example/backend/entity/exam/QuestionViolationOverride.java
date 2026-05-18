package com.example.backend.entity.exam;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "question_violation_override")
public class QuestionViolationOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "override_id")
    private Long overrideId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ExamQuestion question;

    @Column(name = "violation_type", nullable = false, length = 50)
    private String violationType;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "max_allowed_count")
    private Integer maxAllowedCount;

    @Setter
    @Getter
    @Column(name = "severity")
    private String severity;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (isEnabled == null) {
            isEnabled = true;
        }

        if (maxAllowedCount == null) {
            maxAllowedCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getOverrideId() {
        return overrideId;
    }

    public void setOverrideId(Long overrideId) {
        this.overrideId = overrideId;
    }

    public ExamQuestion getQuestion() {
        return question;
    }

    public void setQuestion(ExamQuestion question) {
        this.question = question;
    }

    public Long getQuestionId() {
        return question != null ? question.getQuestionId() : null;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public Integer getMaxAllowedCount() {
        return maxAllowedCount;
    }

    public void setMaxAllowedCount(Integer maxAllowedCount) {
        this.maxAllowedCount = maxAllowedCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}