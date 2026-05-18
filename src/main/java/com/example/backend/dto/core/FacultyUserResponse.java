package com.example.backend.dto.core;

public class FacultyUserResponse {

    private String schoolId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;

    private String registrarStatus;
    private String systemAccess;

    private String loadSummary;

    public FacultyUserResponse() {
    }

    public FacultyUserResponse(String schoolId,
                               String username,
                               String firstName,
                               String lastName,
                               String email,
                               String registrarStatus,
                               String systemAccess,
                               String loadSummary) {
        this.schoolId = schoolId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = buildFullName(firstName, lastName);
        this.email = email;
        this.registrarStatus = registrarStatus;
        this.systemAccess = systemAccess;
        this.loadSummary = loadSummary;
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String full = (first + " " + last).trim();
        return full.isBlank() ? "-" : full;
    }

    public String getSchoolId() { return schoolId; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRegistrarStatus() { return registrarStatus; }
    public String getSystemAccess() { return systemAccess; }
    public String getLoadSummary() { return loadSummary; }
}