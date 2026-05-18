package com.example.backend.repository.core;

import com.example.backend.entity.core.SystemActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemActivityLogRepository extends JpaRepository<SystemActivityLog, Long> {
}
