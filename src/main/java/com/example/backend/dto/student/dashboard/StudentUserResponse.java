package com.example.backend.dto.student.dashboard;

public class StudentUserResponse {

    private String schoolId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String collegeName;
    private String programName;
    private Integer yearLevel;
    private String sectionName;
    private String registrarStatus;
    private String systemAccess;

    public StudentUserResponse() {
    }

    public StudentUserResponse(String schoolId,
                               String username,
                               String firstName,
                               String lastName,
                               String email,
                               String collegeName,
                               String programName,
                               Integer yearLevel,
                               String sectionName,
                               String registrarStatus,
                               String systemAccess) {
        this.schoolId = schoolId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = buildFullName(firstName, lastName);
        this.email = email;
        this.collegeName = collegeName;
        this.programName = programName;
        this.yearLevel = yearLevel;
        this.sectionName = sectionName;
        this.registrarStatus = registrarStatus;
        this.systemAccess = systemAccess;
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
    public String getCollegeName() { return collegeName; }
    public String getProgramName() { return programName; }
    public Integer getYearLevel() { return yearLevel; }
    public String getSectionName() { return sectionName; }
    public String getRegistrarStatus() { return registrarStatus; }
    public String getSystemAccess() { return systemAccess; }
}