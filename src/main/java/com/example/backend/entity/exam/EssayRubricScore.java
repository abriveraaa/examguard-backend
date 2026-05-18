package com.example.backend.entity.exam;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "essay_rubric_score",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_answer_rubric",
                        columnNames = {"answer_id", "rubric_id"}
                )
        }
)
public class EssayRubricScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private ExamAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private EssayRubric rubric;

    @Column(name = "score_awarded", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoreAwarded = BigDecimal.ZERO;

    @Column(name = "score_percentage", precision = 5, scale = 2)
    private BigDecimal scorePercentage = BigDecimal.ZERO;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
