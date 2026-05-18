package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveAnswerRequest {

    private Long attemptId;
    private Long questionId;
    private Long selectedChoiceId;
    private String answerText;

}
