package com.example.backend.dto.faculty.response;

import com.example.backend.dto.faculty.FacultyClassDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class FacultyExamDetailResponse {

    private Long examId;
    private String title;
    private String description;
    private String status;

    private Integer timeLimitMinutes;

    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;

    private Boolean shuffleQuestions;
    private Boolean shuffleChoices;

    private Long totalStudents;
    private Long submittedCount;
    private Long notSubmittedCount;

    private Long studentsWithViolations;
    private Long totalViolations;

    private Boolean resultsReleased;

    private List<FacultyClassDTO> assignedClasses;

    public FacultyExamDetailResponse(
            Long examId,
            String title,
            String description,
            String status,
            Integer timeLimitMinutes,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime,
            Boolean shuffleQuestions,
            Boolean shuffleChoices,
            Long totalStudents,
            Long submittedCount,
            Long notSubmittedCount,
            Long studentsWithViolations,
            Long totalViolations,
            Boolean resultsReleased
    ) {
        this.examId = examId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.timeLimitMinutes = timeLimitMinutes;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.shuffleQuestions = shuffleQuestions;
        this.shuffleChoices = shuffleChoices;
        this.totalStudents = totalStudents;
        this.submittedCount = submittedCount;
        this.notSubmittedCount = notSubmittedCount;
        this.studentsWithViolations = studentsWithViolations;
        this.totalViolations = totalViolations;
        this.resultsReleased = resultsReleased;
    }
}