package com.example.backend.entity.exam;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.swing.text.StyledEditorKit;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "exam_answer",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_attempt_question",
                        columnNames = {"attempt_id", "question_id"}
                )
        }
)
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ExamQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_choice_id")
    private ExamChoice selectedChoiceId;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    private Boolean isCorrect;

    @Column(precision = 10, scale = 2)
    private BigDecimal pointsAwarded = BigDecimal.ZERO;

    @Column(name = "manually_reviewed")
    private Boolean manuallyReviewed = false;

    @Column(name = "needs_checking")
    private Boolean needsChecking = false;

    @Column(name = "review_status", length = 30)
    private String reviewStatus = "AUTO_CHECKED";

    @Column(name = "faculty_feedback", columnDefinition = "TEXT")
    private String facultyFeedback;

    @OneToMany(
            mappedBy = "answer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<EssayRubricScore> rubricScores = new ArrayList<>();

    private OffsetDateTime answeredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;
        answeredAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
        answeredAt = OffsetDateTime.now();
    }

    // getters and setters
}
