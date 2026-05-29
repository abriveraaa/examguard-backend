package com.example.backend.dto.ai;

import java.io.Serializable;
import java.util.List;

public class AiAssetManifestDto implements Serializable {
    private List<AiAssetDto> assets;

    public AiAssetManifestDto() {}

    public AiAssetManifestDto(List<AiAssetDto> assets) {
        this.assets = assets;
    }

    public List<AiAssetDto> getAssets() {
        return assets;
    }
}