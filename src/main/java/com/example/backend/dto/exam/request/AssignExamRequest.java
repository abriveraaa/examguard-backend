package com.example.backend.dto.exam.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class AssignExamRequest implements Serializable {

    private String assignedBy;
    private String assignedByRole;
    private List<String> classOfferingIds;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}