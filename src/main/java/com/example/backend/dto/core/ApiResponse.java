package com.example.backend.dto.core;

import java.io.Serializable;

public class ApiResponse implements Serializable {

    private boolean success;
    private String message;
    private String role;
    private boolean mustChangePassword;
    private String sessionToken;
    private String username;
    private String firstName;
    private String lastName;
    private String schoolId;

    public ApiResponse() {
    }

    // 🔥 SIMPLE RESPONSE (activate, forgot, etc.)
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // 🔹 WITH ROLE + PASSWORD FLAG
    public ApiResponse(boolean success, String message, String role, boolean mustChangePassword) {
        this.success = success;
        this.message = message;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    // 🔹 FULL RESPONSE (login)
    public ApiResponse(boolean success,
                       String message,
                       String username,
                       String schoolId,
                       String role,
                       String firstName,
                       String lastName,
                       boolean mustChangePassword,
                       String sessionToken) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.schoolId = schoolId;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mustChangePassword = mustChangePassword;
        this.sessionToken = sessionToken;
    }

    public String getUsername() { return username; }
    public String getSchoolId() { return schoolId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getRole() { return role; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getSessionToken() { return sessionToken; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setRole(String role) { this.role = role; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}