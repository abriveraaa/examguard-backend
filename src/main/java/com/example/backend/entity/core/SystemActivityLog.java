package com.example.backend.entity.core;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "system_activity_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "target_user_id")
    private String targetUserId;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "module")
    private String module;

    @Column(name = "action")
    private String action;

    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "class_offering_id")
    private String classOfferingId;

    @Column(name = "status")
    private String status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}