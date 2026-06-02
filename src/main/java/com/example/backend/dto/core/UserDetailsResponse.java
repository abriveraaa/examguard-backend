package com.example.backend.dto.core;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class UserDetailsResponse implements Serializable {

    private String schoolId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String collegeName;
    private String programName;
    private Integer yearLevel;
    private String sectionName;
    private String accountStatus;
    private String systemAccess;
    private Integer failedAttempts;
    private String lastLogin;

    public UserDetailsResponse() {
    }

    public UserDetailsResponse(String schoolId,
                               String username,
                               String fullName,
                               String email,
                               String role,
                               String collegeName,
                               String programName,
                               Integer yearLevel,
                               String sectionName,
                               String accountStatus,
                               String systemAccess,
                               Integer failedAttempts,
                               String lastLogin) {
        this.schoolId = schoolId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.collegeName = collegeName;
        this.programName = programName;
        this.yearLevel = yearLevel;
        this.sectionName = sectionName;
        this.accountStatus = accountStatus;
        this.systemAccess = systemAccess;
        this.failedAttempts = failedAttempts;
        this.lastLogin = lastLogin;
    }
}