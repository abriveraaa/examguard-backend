package com.example.backend.examcamera.dto;

public record CameraSessionStatusResponse(
        Long cameraSessionId,
        Long attemptId,
        Long examId,
        String studentId,
        String pairingToken,
        String status,
        String deviceLabel,
        String pairedAt,
        String lastSeenAt,
        String expiresAt
) {}