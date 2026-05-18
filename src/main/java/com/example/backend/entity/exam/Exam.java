package com.example.backend.entity.exam;

import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.enums.ExamStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "exam")
public class Exam {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    private Long examId;

    @Setter
    @Getter
    private String title;

    @Setter
    @Getter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Getter
    @Column(name = "created_by")
    private String createdBy;

    @Setter
    @Column(name = "created_by_role")
    private String createdByRole;

    @Setter
    @Getter
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Setter
    @Getter
    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions = false;

    @Setter
    @Getter
    @Column(name = "shuffle_choices")
    private Boolean shuffleChoices = false;

    @Setter
    @Getter
    @Column(name = "start_datetime", nullable = false)
    private OffsetDateTime startDateTime;

    @Setter
    @Getter
    @Column(name = "end_datetime", nullable = false)
    private OffsetDateTime endDateTime;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_mode", nullable = false)
    private ExamMode examMode;

    @Setter
    @Column(name = "is_published")
    private Boolean published = false;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    private ExamStatus status = ExamStatus.DRAFT;

    @Setter
    @Getter
    @Column(name = "updated_by")
    private String updatedBy;

    @Setter
    @Column(name = "updated_by_role")
    private String updatedByRole;

    @Getter
    @Setter
    @Column(name = "results_released", nullable = false)
    private Boolean resultsReleased = false;

    @Getter
    @Setter
    @Column(name = "results_released_at")
    private OffsetDateTime resultsReleasedAt;

    @Getter
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Getter
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }


}