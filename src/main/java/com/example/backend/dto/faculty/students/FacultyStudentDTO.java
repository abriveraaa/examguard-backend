package com.example.backend.dto.faculty.students;

public record FacultyStudentDTO(
        String studentId,
        String fullName,
        String emailAddress,

        String collegeCode,
        String collegeName,

        String programCode,
        String programName,
        Integer yearLevel,
        String sectionName,

        String courseCode,
        String courseDescription,
        String classOfferingId
) {}