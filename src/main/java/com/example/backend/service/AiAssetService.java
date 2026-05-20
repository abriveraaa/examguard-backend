package com.example.backend.service;

import com.example.backend.dto.ai.AiAssetDto;
import com.example.backend.dto.ai.AiAssetManifestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class AiAssetService {

    @Value("${examguard.ai-assets.dir:uploads/ai}")
    private String aiAssetsDir;

    private Path rootDir;

    @PostConstruct
    public void init() throws Exception {
        rootDir = Paths.get(aiAssetsDir).toAbsolutePath().normalize();
        Files.createDirectories(rootDir);
    }

    public AiAssetManifestDto getManifest(String baseUrl) throws Exception {
        Path manifestPath = rootDir.resolve("manifest.json").normalize();

        if (!Files.exists(manifestPath)) {
            return new AiAssetManifestDto(List.of());
        }

        ObjectMapper mapper = new ObjectMapper();

        AiAssetManifestDto manifest =
                mapper.readValue(manifestPath.toFile(), AiAssetManifestDto.class);

        for (AiAssetDto asset : manifest.getAssets()) {
            Path file = rootDir.resolve(asset.getFileName()).normalize();

            asset.setSha256(Files.exists(file) ? sha256(file) : "");
            asset.setDownloadUrl(baseUrl + "/ai/assets/download/" + asset.getFileName());
        }

        return manifest;
    }

    private AiAssetDto buildAsset(String key, String version, String type, String fileName, String baseUrl) throws Exception {
        Path file = rootDir.resolve(fileName).normalize();

        String sha256 = Files.exists(file) ? sha256(file) : "";

        return new AiAssetDto(
                key,
                version,
                type,
                fileName,
                sha256,
                baseUrl + "/ai/assets/download/" + fileName
        );
    }

    public Resource download(String fileName) throws Exception {
        Path file = rootDir.resolve(fileName).normalize();

        if (!file.startsWith(rootDir)) {
            throw new SecurityException("Invalid file path.");
        }

        if (!Files.exists(file)) {
            throw new NoSuchFileException("AI asset not found: " + fileName);
        }

        return new UrlResource(file.toUri());
    }

    public AiAssetDto upload(String key, String version, String type, MultipartFile multipartFile, String baseUrl) throws Exception {
        String originalName = multipartFile.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name is required.");
        }

        String safeFileName = Paths.get(originalName).getFileName().toString();

        if (!safeFileName.endsWith(".onnx")
                && !safeFileName.endsWith(".task")
                && !safeFileName.endsWith(".json")
                && !safeFileName.endsWith(".xml")) {
            throw new IllegalArgumentException("Only .onnx, .task, .json, and .xml AI assets are allowed.");
        }

        Path target = rootDir.resolve(safeFileName).normalize();

        if (!target.startsWith(rootDir)) {
            throw new SecurityException("Invalid file path.");
        }

        Files.copy(multipartFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return new AiAssetDto(
                key,
                version,
                type,
                safeFileName,
                sha256(target),
                baseUrl + "/ai/assets/download/" + safeFileName
        );
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}