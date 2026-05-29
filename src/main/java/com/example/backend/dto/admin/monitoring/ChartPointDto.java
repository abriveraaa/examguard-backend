package com.example.backend.dto.admin.monitoring;


import lombok.Getter;

import java.io.Serializable;

@Getter
public class ChartPointDto implements Serializable {

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