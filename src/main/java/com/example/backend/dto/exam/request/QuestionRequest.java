package com.example.backend.dto.exam.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class QuestionRequest {

    private String questionType;
    private String questionText;
    private String questionImageUrl;
    private BigDecimal points;
    private String correctAnswer;
    private Integer questionOrder;
    private List<ChoiceRequest> choices;
    private List<EssayRubricRequest> rubrics = new ArrayList<>();
    private String questionInstruction;


    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getQuestionImageUrl() { return questionImageUrl; }
    public void setQuestionImageUrl(String questionImageUrl) { this.questionImageUrl = questionImageUrl; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    public List<ChoiceRequest> getChoices() { return choices; }
    public void setChoices(List<ChoiceRequest> choices) { this.choices = choices; }
    public List<EssayRubricRequest> getRubrics() { return rubrics; }
    public void setRubrics(List<EssayRubricRequest> rubrics) { this.rubrics = rubrics; }
    public String getQuestionInstruction() { return questionInstruction; }
    public void setQuestionInstruction(String questionInstruction) { this.questionInstruction = questionInstruction; }
}