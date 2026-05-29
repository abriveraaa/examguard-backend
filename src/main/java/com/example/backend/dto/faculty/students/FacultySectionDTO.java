package com.example.backend.dto.faculty.students;

import java.io.Serializable;

public record FacultySectionDTO(
        String classOfferingId,
        String programCode,
        Integer yearLevel,
        String sectionName,
        String label
) implements Serializable { }