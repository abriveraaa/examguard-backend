package com.example.backend.dto.faculty;

import java.time.OffsetDateTime;

public class FacultyActivityLogDTO {

    private String logType; // ACTIVITY or VIOLATION

    private Long logId;
    private Long examId;
    private Long attemptId;
    private Long questionId;
    private Integer questionNumber;

    private String studentId;
    private String studentName;

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
        this.module = module;
        this.action = action;
        this.severity = severity;
        this.message = message;
        this.evidenceUrl = evidenceUrl;
        this.durationMs = durationMs;
        this.occurredAt = occurredAt;
    }

    public String getLogType() { return logType; }
    public Long getLogId() { return logId; }
    public Long getExamId() { return examId; }
    public Long getAttemptId() { return attemptId; }
    public Long getQuestionId() { return questionId; }
    public Integer getQuestionNumber() { return questionNumber; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public Long getDurationMs() { return durationMs; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
