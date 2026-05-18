package com.example.backend.dto.admin.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricCardDto {
    private String label;
    private Long value;
    private String helperText;
}