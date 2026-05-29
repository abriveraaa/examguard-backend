package com.example.backend.dto.faculty.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class FacultyUpdateAnswerScoreRequest implements Serializable {
    private BigDecimal pointsAwarded;
}