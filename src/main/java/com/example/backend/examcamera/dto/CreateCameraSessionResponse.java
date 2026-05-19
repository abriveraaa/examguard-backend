package com.example.backend.examcamera.dto;

public record CreateCameraSessionResponse(
        Long cameraSessionId,
        String pairingToken,
        String pairingUrl,
        String status,
        String expiresAt
) {}