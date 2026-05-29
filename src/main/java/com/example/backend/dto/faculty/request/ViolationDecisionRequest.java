package com.example.backend.dto.faculty.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class ViolationDecisionRequest implements Serializable {
    private Long answerId;
    private Long questionId;
    private Long attemptId;

    private String decision;

    private BigDecimal deduction;

    private String feedback;
}