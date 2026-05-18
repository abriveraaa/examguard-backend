package com.example.backend.dto.exam.request;

import java.time.OffsetDateTime;
import java.util.List;

public class AssignExamRequest {

    private String assignedBy;
    private String assignedByRole;
    private List<String> classOfferingIds;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    public String getAssignedByRole() { return assignedByRole; }
    public void setAssignedByRole(String assignedByRole) { this.assignedByRole = assignedByRole; }
    public List<String> getClassOfferingIds() { return classOfferingIds; }
    public void setClassOfferingIds(List<String> classOfferingIds) { this.classOfferingIds = classOfferingIds; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
}