package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class FacultyEssayReviewRequest implements Serializable {

    private Long answerId;
    private String facultyFeedback;
    private List<EssayRubricScoreRequest> rubricScores;

}
