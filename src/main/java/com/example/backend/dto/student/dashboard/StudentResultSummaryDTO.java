package com.example.backend.dto.student.dashboard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentResultSummaryDTO {

    private Long examId;
    private String courseCode;
    private String examTitle;
    private String status;

    public StudentResultSummaryDTO(
            Long examId,
            String courseCode,
            String examTitle,
            String status
    ) {
        this.examId = examId;
        this.courseCode = courseCode;
        this.examTitle = examTitle;
        this.status = status;
    }
}