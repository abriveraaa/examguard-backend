package com.example.backend.dto.auth;

public class LoginResult {

    private boolean success;
    private String message;
    private String username;
    private String firstName;
    private String lastName;
    private String schoolId;
    private String role;
    private String emailAddress;
    private boolean mustChangePassword;
    private String sessionToken;

    public LoginResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResult(boolean success,
                       String message,
                       String username,
                       String schoolId,
                       String role,
                       boolean mustChangePassword,
                       String sessionToken,
                       String firstName,
                       String lastName,
                       String emailAddress){
        this.success = success;
        this.message = message;
        this.username = username;
        this.schoolId = schoolId;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
        this.sessionToken = sessionToken;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getSchoolId() { return schoolId; }
    public String getRole() { return role; }
    public String getEmailAddress() { return emailAddress; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getSessionToken() { return sessionToken; }
}