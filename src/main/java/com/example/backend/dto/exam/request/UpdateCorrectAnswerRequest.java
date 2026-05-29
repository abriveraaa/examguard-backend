package com.example.backend.dto.exam.request;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UpdateCorrectAnswerRequest implements Serializable {

    private String correctAnswer;
}