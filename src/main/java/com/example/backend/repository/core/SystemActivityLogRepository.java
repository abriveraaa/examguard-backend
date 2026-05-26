package com.example.backend.repository.core;

import com.example.backend.entity.core.SystemActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemActivityLogRepository
        extends JpaRepository<SystemActivityLog, Long> {

    List<SystemActivityLog> findTop5ByActorIdOrderByOccurredAtDesc(String actorId);
}