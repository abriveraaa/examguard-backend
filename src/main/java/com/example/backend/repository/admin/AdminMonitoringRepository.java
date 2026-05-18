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
            'Total Activities',
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
            'Critical Events',
            COUNT(l),
            'Failed or critical system events'
        )
        FROM SystemActivityLog l
        WHERE (l.occurredAt >= :startDate)
          AND (l.occurredAt <= :endDate)
          AND (
                UPPER(COALESCE(l.status, '')) IN ('FAILED', 'ERROR', 'CRITICAL')
                OR LOWER(COALESCE(l.message, '')) LIKE '%critical%'
              )
    """)
    MetricCardDto countCriticalEvents(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = """
    SELECT
        to_char(l.occurred_at, 'YYYY') AS label,
        COALESCE(l.actor_role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM system_activity_log l
    WHERE l.occurred_at >= :startDate
      AND l.occurred_at <= :endDate
      AND (:role = 'All Roles' OR l.actor_role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> activityVolumeByYear(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(l.occurred_at, 'YYYY-MM') AS label,
        COALESCE(l.actor_role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM system_activity_log l
    WHERE l.occurred_at >= :startDate
      AND l.occurred_at <= :endDate
      AND (:role = 'All Roles' OR l.actor_role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> activityVolumeByMonth(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(l.occurred_at, 'YYYY-MM-DD') AS label,
        COALESCE(l.actor_role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM system_activity_log l
    WHERE l.occurred_at >= :startDate
      AND l.occurred_at <= :endDate
      AND (:role = 'All Roles' OR l.actor_role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> activityVolumeByDay(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(l.occurred_at, 'YYYY-MM-DD HH24:00') AS label,
        COALESCE(l.actor_role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM system_activity_log l
    WHERE l.occurred_at >= :startDate
      AND l.occurred_at <= :endDate
      AND (:role = 'All Roles' OR l.actor_role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> activityVolumeByHour(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
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
        WHERE (l.occurredAt >= :startDate)
          AND (l.occurredAt <= :endDate)
          AND (
                UPPER(COALESCE(l.status, '')) IN ('FAILED', 'ERROR', 'CRITICAL')
                OR LOWER(COALESCE(l.message, '')) LIKE '%critical%'
              )
        ORDER BY l.occurredAt DESC
    """)
    List<AdminLogRowDto> recentCriticalEvents(
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
            OR LOWER(COALESCE(l.actorId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(l.actorRole, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(l.module, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(l.action, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(l.status, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(l.message, '')) LIKE LOWER(CONCAT('%', :search, '%'))
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