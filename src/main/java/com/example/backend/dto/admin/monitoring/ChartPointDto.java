package com.example.backend.dto.admin.monitoring;


import lombok.Getter;

@Getter
public class ChartPointDto {

    private String label;
    private String category;
    private Long value;

    public ChartPointDto() {
    }

    public ChartPointDto(
            String label,
            String category,
            Long value
    ) {
        this.label = label;
        this.category = category;
        this.value = value;
    }
}