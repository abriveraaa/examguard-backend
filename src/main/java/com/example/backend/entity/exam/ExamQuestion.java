package com.example.backend.entity.exam;

import com.example.backend.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "exam_question")
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_question_question_id_seq")
    @SequenceGenerator(
            name = "exam_question_question_id_seq",
            sequenceName = "exam_question_question_id_seq",
            allocationSize = 50
    )
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_image_url", columnDefinition = "TEXT")
    private String questionImageUrl;

    @Column(name = "points")
    private BigDecimal points;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "question_order")
    private Integer questionOrder;

    @Column(name = "question_instruction")
    private String questionInstruction;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<EssayRubric> rubrics = new ArrayList<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (points == null) {
            points = BigDecimal.ONE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}