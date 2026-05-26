package com.example.backend.dto.faculty.reports;

import java.math.BigDecimal;

public record ClassRecordScoreCellDTO(
        BigDecimal score,
        BigDecimal totalPoints,
        BigDecimal percentage,
        String status
) {
}