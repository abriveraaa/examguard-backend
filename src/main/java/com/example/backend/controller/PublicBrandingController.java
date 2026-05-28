package com.example.backend.controller;

import com.example.backend.dto.core.BrandingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/public")
public class PublicBrandingController {

    @GetMapping("/branding")
    public BrandingResponse getBranding() {
        BrandingResponse response = new BrandingResponse();
        response.setSchoolName("Polytechnic University of the Philippines");
        response.setShortName("PUP");
        response.setLogoUrl("/branding/PUPLogo.png");
        response.setPictureUrl1("/branding/card-default.png");
        response.setPictureUrl2("/branding/card-overlay.png");
        response.setPrimaryColor("#800000");
        response.setSecondaryColor("#D4AF37");
        response.setProjectName("ExamGuard");
        response.setTagline("Secure Digital Examination Platform");
        return response;
    }

    @PostMapping("/upload-profiles")
    public ResponseEntity<String> uploadAiFile(@RequestParam("file") MultipartFile file) throws IOException {

        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "profiles");
        Files.createDirectories(dir);

        Path target = dir.resolve(file.getOriginalFilename());
        file.transferTo(target.toFile());

        return ResponseEntity.ok("Uploaded to " + target);
    }
}