package com.example.backend.dto.faculty.reports;

import java.math.BigDecimal;
import java.util.Map;

public record ClassRecordStudentRowDTO(
        String studentId,
        String studentName,
        String sectionName,
        Map<Long, ClassRecordScoreCellDTO> scoresByExamId,
        BigDecimal averagePercentage
) {
}