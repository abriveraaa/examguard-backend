package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class QuestionRequest implements Serializable {

    private Long questionId;
    private String questionType;
    private String questionText;
    private String questionImageUrl;
    private BigDecimal points;
    private String correctAnswer;
    private Integer questionOrder;
    private List<ChoiceRequest> choices;
    private List<EssayRubricRequest> rubrics = new ArrayList<>();
    private String questionInstruction;

}