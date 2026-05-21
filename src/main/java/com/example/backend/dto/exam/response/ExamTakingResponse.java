package com.example.backend.dto.exam.response;

import com.example.backend.dto.exam.request.ViolationSettingRequest;
import com.example.backend.entity.enums.ExamMode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class ExamTakingResponse {

    private Long attemptId;
    private Long examId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private ExamMode examMode;
    private List<ExamTakingQuestionResponse> questions;
    private List<ViolationSettingRequest> violationSettings;
    private OffsetDateTime serverNow;
    private OffsetDateTime lobbyOpenAt;
    private OffsetDateTime attemptStartedAt;
    private Boolean canBeginExam;
    private Long remainingSeconds;

    public ExamTakingResponse(
            Long attemptId,
            Long examId,
            String title,
            String description,
            Integer timeLimitMinutes,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime,
            ExamMode examMode,
            List<ExamTakingQuestionResponse> questions,
            List<ViolationSettingRequest> violationSettings
    ) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.title = title;
        this.description = description;
        this.timeLimitMinutes = timeLimitMinutes;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.examMode = examMode;
        this.questions = questions;
        this.violationSettings = violationSettings;
    }

}