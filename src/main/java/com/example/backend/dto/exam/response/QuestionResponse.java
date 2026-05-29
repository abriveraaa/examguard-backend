package com.example.backend.dto.exam.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuestionResponse implements Serializable {

    private Long questionId;
    private Integer questionNumber;
    private String questionText;
    private String questionType;
    private String correctAnswer;

    private List<ChoiceResponse> choices = new ArrayList<>();

    public QuestionResponse() {
    }

    // Essay / identification / true-false
    public QuestionResponse(
            Long questionId,
            Integer questionNumber,
            String questionText,
            String questionType,
            String correctAnswer
    ) {
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.questionType = questionType;
        this.correctAnswer = correctAnswer;
    }

    // Multiple choice
    public QuestionResponse(
            Long questionId,
            Integer questionNumber,
            String questionText,
            String questionType,
            String correctAnswer,
            List<ChoiceResponse> choices
    ) {
        this(questionId, questionNumber, questionText, questionType, correctAnswer);
        this.choices = choices;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<ChoiceResponse> getChoices() {
        return choices;
    }
}