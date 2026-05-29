package com.example.backend.dto.exam.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class EssayRubricScoreResponse implements Serializable {

    private Long rubricId;
    private BigDecimal scorePercentage;
    private BigDecimal scoreAwarded;
    private String feedback;

    public EssayRubricScoreResponse() {
    }

    public EssayRubricScoreResponse(
            Long rubricId,
            BigDecimal scorePercentage,
            BigDecimal scoreAwarded,
            String feedback
    ) {
        this.rubricId = rubricId;
        this.scorePercentage = scorePercentage;
        this.scoreAwarded = scoreAwarded;
        this.feedback = feedback;
    }
}
