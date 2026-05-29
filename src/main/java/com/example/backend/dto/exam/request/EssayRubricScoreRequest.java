package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class EssayRubricScoreRequest implements Serializable {

    private Long rubricId;
    private BigDecimal scorePercentage;
    private String feedback;

}
