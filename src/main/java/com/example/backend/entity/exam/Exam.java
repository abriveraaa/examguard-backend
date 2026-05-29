package com.example.backend.entity.exam;

import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.enums.ExamStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "exam")
public class Exam implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    private Long examId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private String createdBy;

    @Setter
    @Column(name = "created_by_role")
    private String createdByRole;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions = false;

    @Column(name = "shuffle_choices")
    private Boolean shuffleChoices = false;

    @Column(name = "start_datetime", nullable = false)
    private OffsetDateTime startDateTime;

    @Column(name = "end_datetime", nullable = false)
    private OffsetDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_mode", nullable = false)
    private ExamMode examMode;

    @Column(name = "is_published")
    private Boolean published = false;

    @Enumerated(EnumType.STRING)
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_by_role")
    private String updatedByRole;

    @Column(name = "results_released", nullable = false)
    private Boolean resultsReleased = false;

    @Column(name = "results_released_at")
    private OffsetDateTime resultsReleasedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "exam", fetch = FetchType.LAZY)
    @OrderBy("questionOrder ASC")
    private List<ExamQuestion> questions = new ArrayList<>();

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