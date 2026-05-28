package com.example.backend.audit;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityData {

    private String targetUserId;
    private String targetRole;

    private Long examId;
    private Long attemptId;
    private Long questionId;

    private String classOfferingId;
    private String metadata;
}