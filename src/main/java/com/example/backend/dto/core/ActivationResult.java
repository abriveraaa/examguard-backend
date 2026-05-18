package com.example.backend.dto.core;

public class ActivationResult {

    private boolean success;
    private String message;
    private String role;

    public ActivationResult(boolean success, String message, String role) {
        this.success = success;
        this.message = message;
        this.role = role;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getRole() {
        return role;
    }
}