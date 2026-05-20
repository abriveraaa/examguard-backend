package com.example.backend.dto.ai;

import java.util.List;

public class AiAssetManifestDto {
    private List<AiAssetDto> assets;

    public AiAssetManifestDto() {}

    public AiAssetManifestDto(List<AiAssetDto> assets) {
        this.assets = assets;
    }

    public List<AiAssetDto> getAssets() {
        return assets;
    }
}