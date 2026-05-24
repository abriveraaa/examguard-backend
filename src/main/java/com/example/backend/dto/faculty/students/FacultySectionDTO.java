package com.example.backend.dto.faculty.students;

public record FacultySectionDTO(
        String classOfferingId,
        String programCode,
        Integer yearLevel,
        String sectionName,
        String label
) {}