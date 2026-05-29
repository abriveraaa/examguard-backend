package com.example.backend.dto.core;

import lombok.Data;

import java.io.Serializable;

@Data
public class CurrentTermDTO implements Serializable {
    private String academicYear;
    private String term;

    public CurrentTermDTO(String academicYear, String term) {
        this.academicYear = academicYear;
        this.term = term;
    }

}
