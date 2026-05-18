package com.example.backend.dto.exam.request;

public class ViolationSettingRequest {

    private String violationType;
    private Boolean enabled;
    private String severity;
    private Integer maxAllowedCount;

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Integer getMaxAllowedCount() {
        return maxAllowedCount;
    }

    public void setMaxAllowedCount(Integer maxAllowedCount) {
        this.maxAllowedCount = maxAllowedCount;
    }
}