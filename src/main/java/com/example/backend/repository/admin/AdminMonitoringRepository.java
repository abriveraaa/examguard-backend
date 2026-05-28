package com.example.backend.repository.admin;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.dto.admin.monitoring.ChartPointDto;
import com.example.backend.dto.admin.monitoring.MetricCardDto;
import com.example.backend.entity.core.SystemActivityLog;
import com.example.backend.entity.exam.ExamViolationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AdminMonitoringRepository extends JpaRepository<SystemActivityLog, Long> {

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.MetricCardDto(
            'System Logs',
            COUNT(l),
            'System activities within selected filters'
        )
        FROM SystemActivityLog l
        WHERE (l.occurredAt >= :startDate)
          AND (l.occurredAt <= :endDate)
    """)
    MetricCardDto countActivities(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.MetricCardDto(
            'Attention Events',
            COUNT(l),
            'Failed, critical, or slow system events'
        )
        FROM SystemActivityLog l
        WHERE (l.occurredAt >= :startDate)
          AND (l.occurredAt <= :endDate)
          AND (
                COALESCE(l.status, '') IN ('FAILED', 'ERROR', 'CRITICAL')
                OR COALESCE(l.message, '') LIKE '%critical%'
                OR COALESCE(l.durationMs, 0) >= 5000
              )
    """)
    MetricCardDto countAttentionEvents(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    // =======================
    // DASHBOARD ADMIN LOGS
    // =======================
    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
            'SYSTEM',
            l.occurredAt,
            null,
            l.actorId,
            l.actorRole,
            l.targetUserId,
            l.targetRole,
            l.module,
            l.action,
            l.status,
            l.message,
            l.examId,
            l.attemptId,
            l.questionId,
            l.durationMs,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL
        )
        FROM SystemActivityLog l
        WHERE (l.occurredAt >= :startDate)
          AND (l.occurredAt <= :endDate)
          AND (l.module <> 'EXAM_TAKING')
        ORDER BY l.occurredAt DESC
    """)
    List<AdminLogRowDto> recentAttentionSystemEvents(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
        'SYSTEM',
        l.occurredAt,
        null,
        l.actorId,
        l.actorRole,
        l.targetUserId,
        l.targetRole,
        l.module,
        l.action,
        l.status,
        l.message,
        l.examId,
        l.attemptId,
        l.questionId,
        l.durationMs,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL
    )
    FROM SystemActivityLog l
    WHERE l.occurredAt >= :startDate
      AND l.occurredAt <= :endDate
      AND (:role = 'All Roles' OR l.actorRole = :role)
      AND (
            :search = ''
            OR l.actorId LIKE CONCAT('%', :search, '%')
            OR l.actorRole LIKE CONCAT('%', :search, '%')
            OR l.module LIKE CONCAT('%', :search, '%')
            OR l.action LIKE CONCAT('%', :search, '%')
            OR l.status LIKE CONCAT('%', :search, '%')
            OR l.message LIKE CONCAT('%', :search, '%')
          )
    ORDER BY l.occurredAt DESC
""")
    List<AdminLogRowDto> findSystemLogsForMonitoring(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role,
            @Param("search") String search
    );


}