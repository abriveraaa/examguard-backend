package com.example.backend.dto.student.result;

import lombok.Getter;

@Getter
public class StudentExamResultRubricResponse {

    private Long rubricId;
    private String criterionName;
    private Double weightPercentage;
    private String description;
    private Integer displayOrder;

    private Double scoreAwarded;
    private Double scorePercentage;
    private String feedback;

    public StudentExamResultRubricResponse(
            Long rubricId,
            String criterionName,
            Double weightPercentage,
            String description,
            Integer displayOrder,
            Double scoreAwarded,
            Double scorePercentage,
            String feedback
    ) {
        this.rubricId = rubricId;
        this.criterionName = criterionName;
        this.weightPercentage = weightPercentage;
        this.description = description;
        this.displayOrder = displayOrder;
        this.scoreAwarded = scoreAwarded;
        this.scorePercentage = scorePercentage;
        this.feedback = feedback;
    }

}