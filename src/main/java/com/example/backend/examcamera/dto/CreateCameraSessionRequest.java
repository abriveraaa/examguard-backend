package com.example.backend.examcamera.dto;

public record CreateCameraSessionRequest(
        Long attemptId,
        Long examId,
        String studentId
) {}