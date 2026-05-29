package com.example.backend.dto.faculty.response;

import com.example.backend.dto.faculty.FacultyAttemptAnswerReviewDTO;
import com.example.backend.dto.faculty.FacultyAttemptViolationDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class FacultyAttemptReviewResponse implements Serializable {

    private Long attemptId;
    private Long examId;

    private String studentId;
    private String studentName;

    private String attemptStatus;
    private Double scorePercentage;

    private Boolean needsChecking;

    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;

    private List<FacultyAttemptAnswerReviewDTO> answers;
    private List<FacultyAttemptViolationDTO> generalViolations;


    public FacultyAttemptReviewResponse(
            Long attemptId,
            Long examId,
            String studentId,
            String studentName,
            String attemptStatus,
            Double scorePercentage,
            Boolean needsChecking,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            List<FacultyAttemptAnswerReviewDTO> answers,
            List<FacultyAttemptViolationDTO> generalViolations
    ) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.attemptStatus = attemptStatus;
        this.scorePercentage = scorePercentage;
        this.needsChecking = needsChecking;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.answers = answers;
        this.generalViolations = generalViolations;
    }
}
