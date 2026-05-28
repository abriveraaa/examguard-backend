package com.example.backend.service.core;

import com.example.backend.entity.core.SystemActivityLog;
import com.example.backend.repository.core.SystemActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SystemActivityLogService {

    private final SystemActivityLogRepository repository;

    public void log(
            String actorId,
            String actorRole,
            String targetUserId,
            String targetRole,
            String module,
            String action,
            String status,
            String message,
            Long examId,
            Long attemptId,
            Long questionId,
            String classOfferingId,
            Long durationMs,
            String ipAddress,
            String userAgent,
            String metadata
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Manila"));

        SystemActivityLog log = SystemActivityLog.builder()
                .actorId(actorId)
                .actorRole(actorRole)
                .targetUserId(targetUserId)
                .targetRole(targetRole)
                .module(module)
                .action(action)
                .status(status)
                .message(message)
                .examId(examId)
                .attemptId(attemptId)
                .questionId(questionId)
                .classOfferingId(classOfferingId)
                .durationMs(durationMs)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(metadata)
                .occurredAt(now)
                .createdAt(now)
                .build();

        repository.save(log);
    }
}