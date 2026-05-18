package com.example.backend.dto.student.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDTO {
    private String firstName;
    private String lastName;
    private String schoolId;
    private String emailAddress;
    private String programCode;
    private String programName;
    private Integer yearLevel;
    private String sectionName;
    private String collegeCode;
    private String collegeName;
    private String currentTerm;
    private String integrityStatus;
    private String integritySubtitle;
    private String profileImageUrl;
}
