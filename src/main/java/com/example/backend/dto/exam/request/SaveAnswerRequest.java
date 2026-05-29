package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SaveAnswerRequest implements Serializable {

    private Long attemptId;
    private Long questionId;
    private Long selectedChoiceId;
    private String answerText;

}
