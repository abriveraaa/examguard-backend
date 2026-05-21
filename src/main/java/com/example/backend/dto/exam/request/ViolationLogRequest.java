package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class ViolationLogRequest {

    private Long attemptId;
    private Long examId;
    private Long questionId;

    private String violationType;
    private String severity;
    private String violationMessage;

    private String evidenceUrl;
    private String evidenceType;
    private String evidenceSource;
    private String evidenceMetadata;

    private Integer attemptNumber;
    private OffsetDateTime occurredAt;

}
