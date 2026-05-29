package com.example.backend.dto.admin.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class MetricCardDto implements Serializable {
    private String label;
    private Long value;
    private String helperText;
}