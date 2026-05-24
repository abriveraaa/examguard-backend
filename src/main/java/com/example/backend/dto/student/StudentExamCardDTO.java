package com.example.backend.dto.student;

import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.exam.Exam;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class StudentExamCardDTO {

    private Long examId;
    private String title;
    private String courseCode;
    private String courseDescription;
    private String term;
    private String academicYear;
    private String classOfferingStatus;
    private ExamMode mode;
    private String faculty;
    private Long questionCount;
    private Integer durationMinutes;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private String attemptStatus;
    private String reviewStatus;
    private Boolean resultsReleased;
    private String status;
    private Boolean actionable;

    public StudentExamCardDTO(
            Long examId,
            String title,
            String courseCode,
            String courseDescription,
            String term,
            String academicYear,
            String classOfferingStatus,
            ExamMode mode,
            String faculty,
            Long questionCount,
            Integer durationMinutes,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime,
            String attemptStatus,
            String reviewStatus,
            Boolean resultsReleased
    ) {
        this.examId = examId;
        this.title = title;
        this.courseCode = courseCode;
        this.courseDescription = courseDescription;
        this.term = term;
        this.academicYear = academicYear;
        this.classOfferingStatus = classOfferingStatus;
        this.mode = mode;
        this.faculty = faculty;
        this.questionCount = questionCount;
        this.durationMinutes = durationMinutes;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.reviewStatus = reviewStatus;
        this.attemptStatus = attemptStatus;
        this.resultsReleased = resultsReleased;
    }
}
