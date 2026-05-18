package com.example.backend.dto.faculty.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ViolationDecisionRequest {
    private Long answerId;
    private Long questionId;
    private Long attemptId;

    private String decision;

    private BigDecimal deduction;

    private String feedback;
}