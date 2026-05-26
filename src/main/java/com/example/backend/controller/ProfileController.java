package com.example.backend.controller;

import com.example.backend.dto.exam.response.ImageUploadResponse;
import com.example.backend.dto.profile.ProfileResponseDTO;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.ProfileService;
import com.example.backend.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponseDTO> getMyProfile(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok( profileService.getMyProfile(user.getSchoolId(), user.getRole())
        );
    }

    @PostMapping(value="/upload-photo", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                profileService.uploadProfilePhoto(
                        user.getSchoolId(),
                        file
                )
        );
    }
}