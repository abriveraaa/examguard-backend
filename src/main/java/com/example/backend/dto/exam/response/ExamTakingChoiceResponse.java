package com.example.backend.dto.exam.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ExamTakingChoiceResponse implements Serializable {
    private Long choiceId;
    private String choiceLabel;
    private String choiceText;
    private Integer choiceOrder;
    private String choiceImageUrl;


    public ExamTakingChoiceResponse() {
    }

    public ExamTakingChoiceResponse(
            Long choiceId,
            String choiceLabel,
            String choiceText,
            Integer choiceOrder,
            String choiceImageUrl
    ) {
        this.choiceId = choiceId;
        this.choiceLabel = choiceLabel;
        this.choiceText = choiceText;
        this.choiceOrder = choiceOrder;
        this.choiceImageUrl = choiceImageUrl;
    }

}
