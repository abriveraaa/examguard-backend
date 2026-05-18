package com.example.backend.entity.exam;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_attempt_choice_order")
public class ExamAttemptChoiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "choice_order", columnDefinition = "TEXT", nullable = false)
    private String choiceOrder;

    public Long getId() { return id; }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getChoiceOrder() { return choiceOrder; }
    public void setChoiceOrder(String choiceOrder) { this.choiceOrder = choiceOrder; }
}
