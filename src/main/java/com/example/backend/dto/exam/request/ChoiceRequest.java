package com.example.backend.dto.exam.request;

public class ChoiceRequest {

    private String choiceLabel;
    private String choiceText;
    private String choiceImageUrl;
    private Boolean correct;
    private Integer choiceOrder;



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