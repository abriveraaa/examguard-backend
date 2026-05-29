package com.example.backend.dto.exam.response;

import java.io.Serializable;

public class ChoiceResponse implements Serializable {

    private Long choiceId;
    private String choiceText;
    private boolean correct;

    public ChoiceResponse() {
    }

    public ChoiceResponse(Long choiceId, String choiceText, boolean correct) {
        this.choiceId = choiceId;
        this.choiceText = choiceText;
        this.correct = correct;
    }

    public Long getChoiceId() {
        return choiceId;
    }

    public String getChoiceText() {
        return choiceText;
    }

    public boolean isCorrect() {
        return correct;
    }
}