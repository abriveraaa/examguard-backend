package com.example.backend.examcamera;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "exam_camera_session")
public class ExamCameraSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "camera_session_id")
    private Long cameraSessionId;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false, length = 50)
    private String studentId;

    @Column(name = "pairing_token", nullable = false, unique = true, length = 120)
    private String pairingToken;

    @Column(name = "device_label")
    private String deviceLabel;

    @Column(name = "device_type", nullable = false, length = 30)
    private String deviceType = "PHONE";

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "required_angle", length = 30)
    private String requiredAngle = "SIDE_45_DEGREE";

    @Column(name = "stream_role", length = 30)
    private String streamRole = "STRICT_PROCTORING";

    @Column(name = "paired_at")
    private OffsetDateTime pairedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "disconnect_count", nullable = false)
    private Integer disconnectCount = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = "PENDING";
        if (deviceType == null) deviceType = "PHONE";
        if (requiredAngle == null) requiredAngle = "SIDE_45_DEGREE";
        if (streamRole == null) streamRole = "STRICT_PROCTORING";
        if (disconnectCount == null) disconnectCount = 0;
    }

}