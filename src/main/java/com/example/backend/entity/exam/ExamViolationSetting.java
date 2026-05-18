package com.example.backend.entity.exam;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "exam_violation_setting")
public class ExamViolationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Setter
    @Column(name = "violation_type", nullable = false)
    private String violationType;

    @Setter
    @Column(name = "is_enabled")
    private Boolean enabled;

    @Column(name = "max_allowed_count")
    private Integer maxAllowedCount;

    @Column(name = "severity")
    private String severity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (enabled == null) enabled = true;
        if (severity == null || severity.isBlank()) severity = "MINOR";
        if (maxAllowedCount == null) maxAllowedCount = 0;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (enabled == null) enabled = true;
        if (severity == null || severity.isBlank()) severity = "MINOR";
        if (maxAllowedCount == null) maxAllowedCount = 0;
    }

    public Long getExamId() {
        return exam != null ? exam.getExamId() : null;
    }
}