package com.example.backend.dto.admin.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.OffsetDateTime;

@Data
@Getter
@AllArgsConstructor
public class AdminLogRowDto {

    private String source;

    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;

    private String actorId;
    private String actorRole;

    private String targetUserId;
    private String targetRole;

    private String module;
    private String action;
    private String status;

    private String message;

    private Long examId;
    private Long attemptId;
    private Long questionId;

    private Long durationMs;

    private String programCode;
    private String programName;
    private String section;

    private String severity;

    private String courseCode;
    private String examTitle;
    private Integer questionNumber;
}