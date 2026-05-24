package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChoiceRequest {

    private Long choiceId;
    private String choiceLabel;
    private String choiceText;
    private String choiceImageUrl;
    private Boolean correct;
    private Integer choiceOrder;
}