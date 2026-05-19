package com.example.backend.examcamera;

import com.example.backend.examcamera.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class ExamCameraService {

    private final ExamCameraSessionRepository sessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url}")
    private String appBaseUrl;

    public ExamCameraService(ExamCameraSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public CreateCameraSessionResponse createSession(CreateCameraSessionRequest request) {
        if (request.attemptId() == null) {
            throw new IllegalArgumentException("attemptId is required.");
        }

        if (request.examId() == null) {
            throw new IllegalArgumentException("examId is required.");
        }

        if (request.studentId() == null || request.studentId().isBlank()) {
            throw new IllegalArgumentException("studentId is required.");
        }

        ExamCameraSession session = new ExamCameraSession();
        session.setAttemptId(request.attemptId());
        session.setExamId(request.examId());
        session.setStudentId(request.studentId());
        session.setPairingToken(generateToken());
        session.setStatus("PENDING");
        session.setExpiresAt(OffsetDateTime.now().plusMinutes(10));

        ExamCameraSession saved = sessionRepository.save(session);

        String pairingUrl = appBaseUrl + "/camera-pair/" + saved.getPairingToken();

        return new CreateCameraSessionResponse(
                saved.getCameraSessionId(),
                saved.getPairingToken(),
                pairingUrl,
                saved.getStatus(),
                saved.getExpiresAt().toString()
        );
    }


    public CameraSessionStatusResponse getStatus(String token) {
        ExamCameraSession session = findByToken(token);

        if (isExpired(session) && !"EXPIRED".equals(session.getStatus())) {
            session.setStatus("EXPIRED");
            sessionRepository.save(session);
        }

        return toStatusResponse(session);
    }

    @Transactional
    public CameraSessionStatusResponse pairPhone(String token, PairCameraRequest request) {
        ExamCameraSession session = findByToken(token);

        if (isExpired(session)) {
            session.setStatus("EXPIRED");
            sessionRepository.save(session);
            throw new IllegalStateException("Pairing session already expired.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        session.setStatus("PAIRED");
        session.setPairedAt(now);
        session.setLastSeenAt(now);

        if (request != null && request.deviceLabel() != null && !request.deviceLabel().isBlank()) {
            session.setDeviceLabel(request.deviceLabel());
        } else {
            session.setDeviceLabel("Student Phone");
        }

        return toStatusResponse(sessionRepository.save(session));
    }

    @Transactional
    public CameraSessionStatusResponse heartbeat(String token) {
        ExamCameraSession session = findByToken(token);

        if (isExpired(session)) {
            session.setStatus("EXPIRED");
            return toStatusResponse(sessionRepository.save(session));
        }

        OffsetDateTime now = OffsetDateTime.now();

        session.setLastSeenAt(now);

        if ("PAIRED".equals(session.getStatus())) {
            session.setStatus("ACTIVE");
            session.setStartedAt(now);
        }

        return toStatusResponse(sessionRepository.save(session));
    }

    @Transactional
    public CameraSessionStatusResponse endSession(String token) {
        ExamCameraSession session = findByToken(token);

        session.setStatus("ENDED");
        session.setEndedAt(OffsetDateTime.now());

        return toStatusResponse(sessionRepository.save(session));
    }

    private ExamCameraSession findByToken(String token) {
        return sessionRepository.findByPairingToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Camera session not found."));
    }

    private boolean isExpired(ExamCameraSession session) {
        return session.getExpiresAt() != null && OffsetDateTime.now().isAfter(session.getExpiresAt());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private CameraSessionStatusResponse toStatusResponse(ExamCameraSession session) {
        return new CameraSessionStatusResponse(
                session.getCameraSessionId(),
                session.getAttemptId(),
                session.getExamId(),
                session.getStudentId(),
                session.getPairingToken(),
                session.getStatus(),
                session.getDeviceLabel(),
                session.getPairedAt() == null ? null : session.getPairedAt().toString(),
                session.getLastSeenAt() == null ? null : session.getLastSeenAt().toString(),
                session.getExpiresAt() == null ? null : session.getExpiresAt().toString()
        );
    }
}