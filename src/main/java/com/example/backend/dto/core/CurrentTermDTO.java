package com.example.backend.dto.core;

import lombok.Data;

@Data
public class CurrentTermDTO {
    private String academicYear;
    private String term;

    public CurrentTermDTO(String academicYear, String term) {
        this.academicYear = academicYear;
        this.term = term;
    }

}
