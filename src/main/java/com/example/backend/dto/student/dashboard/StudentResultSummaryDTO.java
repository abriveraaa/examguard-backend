package com.example.backend.dto.student.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class StudentResultSummaryDTO implements Serializable {

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