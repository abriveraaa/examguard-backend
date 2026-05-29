package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class ExamRequest implements Serializable {

    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Boolean shuffleQuestions;
    private Boolean shuffleChoices;
    private String createdBy;
    private String createdByRole;

    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private String examMode;

    private List<String> classOfferingIds;
    private List<QuestionRequest> questions;
    private List<ViolationSettingRequest> violationSettings;
}