package com.example.backend.dto.core;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BrandingResponse implements Serializable {

    private String projectName;
    private String schoolName;
    private String shortName;
    private String logoUrl;
    private String pictureUrl1;
    private String pictureUrl2;
    private String primaryColor;
    private String secondaryColor;
    private String tagline;

}
