package com.example.backend.dto.student.result;

import lombok.Getter;

@Getter
public class StudentExamResultChoiceResponse {

    private Long choiceId;
    private String choiceLabel;
    private String choiceText;
    private String imageUrl;
    private Boolean correct;
    private Boolean selected;

    public StudentExamResultChoiceResponse(
            Long choiceId,
            String choiceLabel,
            String choiceText,
            String imageUrl,
            Boolean correct,
            Boolean selected
    ) {
        this.choiceId = choiceId;
        this.choiceLabel = choiceLabel;
        this.choiceText = choiceText;
        this.imageUrl = imageUrl;
        this.correct = correct;
        this.selected = selected;
    }

}
