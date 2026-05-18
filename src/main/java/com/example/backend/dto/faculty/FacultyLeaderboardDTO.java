package com.example.backend.dto.faculty;


import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class FacultyLeaderboardDTO {

    private Integer rank;
    private Long attemptId;
    private String studentId;
    private String firstName;
    private String lastName;
    private String studentName;
    private String sectionName;
    private Double totalScore;
    private Double totalPossibleScore;
    private Double scorePercentage;
    private Long violationCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private String integrityStatus;

    public FacultyLeaderboardDTO(
            Integer rank,
            Long attemptId,
            String studentId,
            String studentName,
            String sectionName,
            Double totalScore,
            Double totalPossibleScore,
            Double scorePercentage,
            Long violationCount,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            String integrityStatus
    ) {
        this.rank = rank;
        this.attemptId = attemptId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.sectionName = sectionName;
        this.totalScore = totalScore;
        this.totalPossibleScore = totalPossibleScore;
        this.scorePercentage = scorePercentage;
        this.violationCount = violationCount;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.integrityStatus = integrityStatus;
    }

    public FacultyLeaderboardDTO(
            Long attemptId,
            String studentId,
            String studentName,
            String sectionName,
            Double totalScore,
            Double scorePercentage,
            Long violationCount,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            String integrityStatus
    ) {
        this.attemptId = attemptId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.sectionName = sectionName;
        this.totalScore = totalScore;
        this.scorePercentage = scorePercentage;
        this.violationCount = violationCount;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.integrityStatus = integrityStatus;
    }

    // getters and setters
}
