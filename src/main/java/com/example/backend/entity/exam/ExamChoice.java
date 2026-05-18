package com.example.backend.entity.exam;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_choice")
public class ExamChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_choice_choice_id_seq")
    @SequenceGenerator(
            name = "exam_choice_choice_id_seq",
            sequenceName = "exam_choice_choice_id_seq",
            allocationSize = 50
    )
    @Column(name = "choice_id")
    private Long choiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ExamQuestion question;

    @Column(name = "choice_label")
    private String choiceLabel;

    @Column(name = "choice_text", columnDefinition = "TEXT")
    private String choiceText;

    @Column(name = "choice_image_url", columnDefinition = "TEXT")
    private String choiceImageUrl;

    @Column(name = "is_correct")
    private Boolean correct = false;

    @Column(name = "choice_order")
    private Integer choiceOrder;

    public Long getChoiceId() { return choiceId; }

    public ExamQuestion getQuestion() { return question; }
    public Long getQuestionId() { return question != null ? question.getQuestionId() : null; }
    public void setQuestion(ExamQuestion question) { this.question = question; }

    public String getChoiceLabel() { return choiceLabel; }
    public void setChoiceLabel(String choiceLabel) { this.choiceLabel = choiceLabel; }

    public String getChoiceText() { return choiceText; }
    public void setChoiceText(String choiceText) { this.choiceText = choiceText; }

    public String getChoiceImageUrl() { return choiceImageUrl; }
    public void setChoiceImageUrl(String choiceImageUrl) { this.choiceImageUrl = choiceImageUrl; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    public Integer getChoiceOrder() { return choiceOrder; }
    public void setChoiceOrder(Integer choiceOrder) { this.choiceOrder = choiceOrder; }
}