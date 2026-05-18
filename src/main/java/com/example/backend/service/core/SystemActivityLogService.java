package com.example.backend.service.core;

import com.example.backend.entity.core.SystemActivityLog;
import com.example.backend.repository.core.SystemActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemActivityLogService {

    private final SystemActivityLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String actorId,
            String actorRole,
            String module,
            String action,
            String status,
            String message,

            Long examId,
            Long attemptId,
            Long questionId,

            Long durationMs
    ) {

        SystemActivityLog log = SystemActivityLog.builder()
                .actorId(actorId)
                .actorRole(actorRole)

                .module(module)
                .action(action)

                .status(status)
                .message(message)

                .examId(examId)
                .attemptId(attemptId)
                .questionId(questionId)

                .durationMs(durationMs)

                .occurredAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        repository.save(log);
    }

    public void logExamQuestionActivity(
            String actorId,
            String actorRole,
            Long examId,
            Long attemptId,
            Long questionId,
            String action,
            String message,
            Long durationMs,
            String metadata
    ) {

        SystemActivityLog log = new SystemActivityLog();

        log.setActorId(actorId);
        log.setActorRole(actorRole);

        log.setModule("EXAM_TAKING");
        log.setAction(action);

        log.setExamId(examId);
        log.setAttemptId(attemptId);
        log.setQuestionId(questionId);

        log.setStatus("SUCCESS");
        log.setMessage(message);

        log.setDurationMs(durationMs);

        log.setMetadata(metadata);
        log.setOccurredAt(OffsetDateTime.now());
        log.setCreatedAt(OffsetDateTime.now());

        repository.save(log);
    }
}
