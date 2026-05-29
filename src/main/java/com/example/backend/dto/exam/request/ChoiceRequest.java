package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ChoiceRequest implements Serializable {

    private Long choiceId;
    private String choiceLabel;
    private String choiceText;
    private String choiceImageUrl;
    private Boolean correct;
    private Integer choiceOrder;
}