package com.example.backend.dto.core;

import java.io.Serializable;
import java.time.LocalDate;

public class AdminUserResponse implements Serializable {

    private String schoolId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private LocalDate birthDate;
    private String registrarStatus;
    private String systemAccess;

    public AdminUserResponse() {
    }

    public AdminUserResponse(String schoolId,
                             String username,
                             String firstName,
                             String lastName,
                             String email,
                             LocalDate birthDate,
                             String registrarStatus,
                             String systemAccess) {
        this.schoolId = schoolId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = buildFullName(firstName, lastName);
        this.email = email;
        this.birthDate = birthDate;
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
    public LocalDate getBirthDate() { return birthDate; }
    public String getRegistrarStatus() { return registrarStatus; }
    public String getSystemAccess() { return systemAccess; }
}