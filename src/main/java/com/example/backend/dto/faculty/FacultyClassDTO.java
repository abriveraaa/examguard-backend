package com.example.backend.dto.faculty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacultyClassDTO {

    private String classOfferingId;
    private String courseCode;
    private String courseDescription;
    private String programCode;
    private String sectionName;
    private Integer yearLevel;
    private String academicYear;
    private String term;
    private Long enrolledCount;

    public FacultyClassDTO(
            String classOfferingId,
            String courseCode,
            String courseDescription,
            String programCode,
            String sectionName,
            Integer yearLevel,
            String academicYear,
            String term,
            Long enrolledCount
    ) {
        this.classOfferingId = classOfferingId;
        this.courseCode = courseCode;
        this.courseDescription = courseDescription;
        this.programCode = programCode;
        this.sectionName = sectionName;
        this.yearLevel = yearLevel;
        this.academicYear = academicYear;
        this.term = term;
        this.enrolledCount = enrolledCount;
    }

    // getters and setters
}
