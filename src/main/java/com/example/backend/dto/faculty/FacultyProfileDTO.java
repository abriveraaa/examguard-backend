package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FacultyProfileDTO implements Serializable {

    private String employeeId;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String collegeCode;
    private String collegeName;

    public FacultyProfileDTO() {
    }

    public FacultyProfileDTO(
            String employeeId,
            String firstName,
            String lastName,
            String emailAddress
    ) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
    }

    public String getFullName() {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

}