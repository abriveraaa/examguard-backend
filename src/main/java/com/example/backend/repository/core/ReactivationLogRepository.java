package com.example.backend.repository.core;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.entity.core.ReactivationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface ReactivationLogRepository extends JpaRepository<ReactivationLog, Long> {

    @Query("""
    SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
        'REACTIVATION',
        r.reactivatedAt,
        null,
        r.schoolId,
        r.role,
        r.schoolId,
        r.role,
        'ACCOUNT',
        'REACTIVATION_REQUEST',
        'SUCCESS',
        r.justification,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL
    )
    FROM ReactivationLog r
    WHERE r.reactivatedAt >= :startDate
      AND r.reactivatedAt <= :endDate
      AND (
            :search = ''
            OR r.schoolId LIKE CONCAT('%', :search, '%')
            OR r.role LIKE CONCAT('%', :search, '%')
            OR r.justification LIKE CONCAT('%', :search, '%')
          )
    ORDER BY r.reactivatedAt DESC
""")
    List<AdminLogRowDto> findReactivationLogsForMonitoring(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("search") String search
    );
}