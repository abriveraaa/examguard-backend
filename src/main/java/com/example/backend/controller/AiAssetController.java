package com.example.backend.controller;

import com.example.backend.dto.ai.AiAssetDto;
import com.example.backend.dto.ai.AiAssetManifestDto;
import com.example.backend.service.AiAssetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/ai/assets")
public class AiAssetController {

    private final AiAssetService aiAssetService;

    public AiAssetController(AiAssetService aiAssetService) {
        this.aiAssetService = aiAssetService;
    }

    @GetMapping("/manifest")
    public ResponseEntity<AiAssetManifestDto> getManifest(HttpServletRequest request) throws Exception {
        String baseUrl = getBaseUrl();
        return ResponseEntity.ok(aiAssetService.getManifest(baseUrl));
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) throws Exception {
        Resource resource = aiAssetService.download(fileName);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\""
                )
                .body(resource);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiAssetDto> upload(
            @RequestParam String key,
            @RequestParam String version,
            @RequestParam String type,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        String baseUrl = getBaseUrl();
        return ResponseEntity.ok(
                aiAssetService.upload(key, version, type, file, baseUrl)
        );
    }

    private String getBaseUrl() {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .build()
                .toUriString();
    }
}