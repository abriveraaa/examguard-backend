package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class EssayRubricRequest implements Serializable {

    private Long rubricId;
    private String criterionName;
    private BigDecimal weightPercentage;
    private String description;
    private Integer displayOrder;

}