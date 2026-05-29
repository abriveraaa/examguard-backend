package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ExamActivityRequest implements Serializable {
    private Long examId;
    private Long attemptId;
    private Long questionId;
    private String action;
    private String message;
    private Long durationMs;
    private String metadata;

}
