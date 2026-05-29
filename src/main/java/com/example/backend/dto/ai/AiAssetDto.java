package com.example.backend.dto.ai;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AiAssetDto implements Serializable {
    private String key;
    private String version;
    private String type;
    private String fileName;
    private String sha256;
    private String downloadUrl;

    public AiAssetDto() {}

    public AiAssetDto(String key, String version, String type, String fileName, String sha256, String downloadUrl) {
        this.key = key;
        this.version = version;
        this.type = type;
        this.fileName = fileName;
        this.sha256 = sha256;
        this.downloadUrl = downloadUrl;
    }

}