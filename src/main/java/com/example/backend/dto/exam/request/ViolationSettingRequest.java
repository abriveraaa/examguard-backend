package com.example.backend.dto.exam.request;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ViolationSettingRequest implements Serializable {

    private String violationType;
    private Boolean enabled;
    private String severity;
    private Integer maxAllowedCount;
}