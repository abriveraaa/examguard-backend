package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FacultyDashboardStatsDTO implements Serializable {

    private Long activeExamCount;
    private Long classOfferingCount;
    private Long totalStudentCount;
    private Long reviewQueueCount;

    public FacultyDashboardStatsDTO(
            Long activeExamCount,
            Long classOfferingCount,
            Long totalStudentCount,
            Long reviewQueueCount
    ) {
        this.activeExamCount = activeExamCount;
        this.classOfferingCount = classOfferingCount;
        this.totalStudentCount = totalStudentCount;
        this.reviewQueueCount = reviewQueueCount;
    }

}
