package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
public class FacultyViolationReviewDTO implements Serializable {

    private Long examId;
    private String examTitle;
    private String courseCode;
    private Long studentCount;
    private Long violationCount;
    private OffsetDateTime latestViolationAt;

    public FacultyViolationReviewDTO(
            Long examId,
            String examTitle,
            String courseCode,
            Long studentCount,
            Long violationCount,
            OffsetDateTime latestViolationAt
    ) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.courseCode = courseCode;
        this.studentCount = studentCount;
        this.violationCount = violationCount;
        this.latestViolationAt = latestViolationAt;
    }

    // getters and setters
}

