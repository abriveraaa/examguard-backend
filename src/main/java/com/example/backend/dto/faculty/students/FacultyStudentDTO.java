package com.example.backend.dto.faculty.students;

public record FacultyStudentDTO(
        String studentId,
        String firstName,
        String lastName,
        String emailAddress,

        String collegeCode,
        String collegeName,

        String programCode,
        String programName,
        Integer yearLevel,
        String sectionName,

        String courseCode,
        String courseDescription,
        String classOfferingId,
        String profileImageUrl
) {}