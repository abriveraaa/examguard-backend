package com.example.backend.repository.core;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.entity.core.RegistrarSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistrarSyncLogRepository extends JpaRepository<RegistrarSyncLog, Long> {
    Optional<RegistrarSyncLog> findTopByStatusOrderByFinishedAtDesc(String status);

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
            'REGISTRAR',
            l.startedAt,
            l.finishedAt,
            l.performedBy,
            'ADMIN',
            NULL,
            NULL,
            'REGISTRAR_SYNC',
            l.syncType,
            l.status,
            l.message,
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
        FROM RegistrarSyncLog l
        WHERE COALESCE(l.finishedAt, l.startedAt) >= :startDate
          AND COALESCE(l.finishedAt, l.startedAt) <= :endDate
          AND (
                :search = ''
                OR LOWER(COALESCE(l.syncType, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.status, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.performedBy, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.message, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
        ORDER BY COALESCE(l.finishedAt, l.startedAt) DESC
    """)
    List<AdminLogRowDto> findRegistrarLogsForMonitoring(
                @Param("startDate") OffsetDateTime startDate,
                @Param("endDate") OffsetDateTime endDate,
                @Param("search") String search
        );
}
