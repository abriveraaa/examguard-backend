package com.example.backend.dto.admin.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class AdminUserRowDto implements Serializable {

    private String schoolId;
    private String username;
    private String fullName;
    private String role;
    private String collegeCode;
    private String programCode;
    private Integer yearLevel;
    private String email;
    private String registrarStatus;
    private String systemAccess;

    public AdminUserRowDto(
            String schoolId,
            String username,
            String fullName,
            String email,
            String registrarStatus,
            String systemAccess
    ) {
        this.schoolId = schoolId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.registrarStatus = registrarStatus;
        this.systemAccess = systemAccess;
    }

    public AdminUserRowDto(
            String schoolId,
            String username,
            String fullName,
            String email,
            String collegeCode,
            String programCode,
            Integer yearLevel,
            String registrarStatus,
            String systemAccess
    ) {
        this.schoolId = schoolId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.collegeCode = collegeCode;
        this.programCode = programCode;
        this.yearLevel = yearLevel;
        this.registrarStatus = registrarStatus;
        this.systemAccess = systemAccess;
    }
}