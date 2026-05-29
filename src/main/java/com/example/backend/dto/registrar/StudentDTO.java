package com.example.backend.dto.registrar;

import java.io.Serializable;

public class StudentDTO implements Serializable {
    public String studentId;
    public String firstName;
    public String lastName;
    public String birthDate;
    public String emailAddress;
    public String collegeCode;
    public String collegeName;
    public String programCode;
    public String programName;
    public Integer yearLevel;
    public String sectionName;
    public String scholasticStatus;
    public String updatedAt;
}