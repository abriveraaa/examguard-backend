package com.example.backend.report.exam.dto;

public record ScoreDistributionDTO(
        String rangeLabel,
        Long studentCount
) {
}