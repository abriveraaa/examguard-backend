package com.example.backend.dto.core;

public class DeactivateRequest {

    private String reason;
    private String currentAdminId;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCurrentAdminId() {
        return currentAdminId;
    }

    public void setCurrentAdminId(String currentAdminId) {
        this.currentAdminId = currentAdminId;
    }
}