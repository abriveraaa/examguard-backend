package com.example.backend.dto.faculty;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class FacultyActivityLogDTO {

    private String logType;
    private Long logId;
    private Long examId;
    private Long attemptId;
    private Long questionId;
    private Integer questionNumber;
    private String studentId;
    private String studentName;
    private String actorId;
    private String actorName;
    private String module;
    private String action;
    private String severity;
    private String message;
    private String evidenceUrl;

    private Long durationMs;
    private OffsetDateTime occurredAt;

    public FacultyActivityLogDTO(
            String logType,
            Long logId,
            Long examId,
            Long attemptId,
            Long questionId,
            Integer questionNumber,
            String studentId,
            String studentName,
            String actorId,
            String actorName,
            String module,
            String action,
            String severity,
            String message,
            String evidenceUrl,
            Long durationMs,
            OffsetDateTime occurredAt
    ) {
        this.logType = logType;
        this.logId = logId;
        this.examId = examId;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.studentId = studentId;
        this.studentName = studentName;
        this.actorId = actorId;
        this.actorName = actorName;
        this.module = module;
        this.action = action;
        this.severity = severity;
        this.message = message;
        this.evidenceUrl = evidenceUrl;
        this.durationMs = durationMs;
        this.occurredAt = occurredAt;
    }
}
