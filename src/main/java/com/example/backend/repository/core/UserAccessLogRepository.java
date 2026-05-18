package com.example.backend.repository.core;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.entity.core.UserAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {
    Optional<UserAccessLog> findFirstBySchoolIdAndEventTypeAndEventStatusOrderByCreatedAtDesc(
            String schoolId,
            String eventType,
            String eventStatus
    );

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
            'ACCESS',
            l.createdAt,
            null,
            l.schoolId,
            u.role,
            NULL,
            NULL,
            'AUTH',
            l.eventType,
            l.eventStatus,
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
        FROM UserAccessLog l
        JOIN UserAccess u ON u.schoolId = l.schoolId
        WHERE l.createdAt >= :startDate
          AND l.createdAt <= :endDate
          AND (
                :search = ''
                OR LOWER(COALESCE(l.schoolId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.eventType, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.eventStatus, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.message, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.ipAddress, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
        ORDER BY l.createdAt DESC
    """)
    List<AdminLogRowDto> findAccessLogsForMonitoring(
                @Param("startDate") OffsetDateTime startDate,
                @Param("endDate") OffsetDateTime endDate,
                @Param("search") String search
        );
}