package com.example.backend.dto.faculty;

import com.example.backend.entity.enums.ExamMode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class FacultyExamSummaryDTO {

    private Long examId;
    private String title;
    private String courseCode;
    private String courseDescription;
    private String programCode;
    private String classSections;
    private String status;
    private ExamMode examMode;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private Long totalAssigned;
    private Long submittedCount;
    private Long violationCount;

    public FacultyExamSummaryDTO(
            Long examId,
            String title,
            String courseCode,
            String courseDescription,
            String programCode,
            String classSections,
            String status,
            ExamMode examMode,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime,
            Long totalAssigned,
            Long submittedCount,
            Long violationCount
    ) {
        this.examId = examId;
        this.title = title;
        this.courseCode = courseCode;
        this.courseDescription = courseDescription;
        this.programCode = programCode;
        this.classSections = classSections;
        this.status = status;
        this.examMode = examMode;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.totalAssigned = totalAssigned;
        this.submittedCount = submittedCount;
        this.violationCount = violationCount;
    }
}