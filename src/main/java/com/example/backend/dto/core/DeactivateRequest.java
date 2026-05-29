package com.example.backend.dto.core;

import java.io.Serializable;

public class DeactivateRequest implements Serializable {

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