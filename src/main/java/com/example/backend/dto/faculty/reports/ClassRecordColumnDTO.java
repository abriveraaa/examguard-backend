package com.example.backend.dto.faculty.reports;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ClassRecordColumnDTO(
        Long examId,
        String examCode,
        String examTitle,
        BigDecimal totalPoints,
        OffsetDateTime startDateTime,
        OffsetDateTime endDateTime
) {
}