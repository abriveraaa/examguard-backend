package com.example.backend.dto.faculty.students;

import java.io.Serializable;

public record FacultyCourseDTO(
        String courseCode,
        String courseDescription
) implements Serializable {}