package com.example.backend.examcamera;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamCameraSessionRepository extends JpaRepository<ExamCameraSession, Long> {

    Optional<ExamCameraSession> findByPairingToken(String pairingToken);

    Optional<ExamCameraSession> findTopByAttemptIdOrderByCreatedAtDesc(Long attemptId);
}