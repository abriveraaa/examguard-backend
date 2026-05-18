package com.example.backend.dto.student;

import lombok.Getter;

@Getter
public class StudentResultHeaderDTO {

    private String courseCode;
    private String courseDescription;
    private String faculty;
    private String term;
    private String academicYear;

    public StudentResultHeaderDTO(
            String courseCode,
            String courseDescription,
            String faculty,
            String term,
            String academicYear
    ) {
        this.courseCode = courseCode;
        this.courseDescription = courseDescription;
        this.faculty = faculty;
        this.term = term;
        this.academicYear = academicYear;
    }
}
