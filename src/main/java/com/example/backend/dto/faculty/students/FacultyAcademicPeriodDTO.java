package com.example.backend.dto.faculty.students;

import java.io.Serializable;

public record FacultyAcademicPeriodDTO(
        String academicYear,
        String term,
        String classOfferingStatus
) implements Serializable {}