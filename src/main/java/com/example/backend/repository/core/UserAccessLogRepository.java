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
                OR l.schoolId LIKE CONCAT('%', :search, '%')
                OR l.username LIKE CONCAT('%', :search, '%')
                OR l.eventType LIKE CONCAT('%', :search, '%')
                OR l.eventStatus LIKE CONCAT('%', :search, '%')
                OR l.message LIKE CONCAT('%', :search, '%')
                OR l.ipAddress LIKE CONCAT('%', :search, '%')
              )
        ORDER BY l.createdAt DESC
    """)
    List<AdminLogRowDto> findAccessLogsForMonitoring(
                @Param("startDate") OffsetDateTime startDate,
                @Param("endDate") OffsetDateTime endDate,
                @Param("search") String search
        );


    // ===============
    // LOGIN COUNT
    // ===============

    @Query(value = """
    SELECT
        to_char(ual.created_at AT TIME ZONE 'Asia/Manila', 'YYYY') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_access_log ual
    LEFT JOIN user_access ua
        ON ua.access_id = ual.access_id
    WHERE ual.created_at >= :startDate
      AND ual.created_at <= :endDate
      AND ual.event_type = 'LOGIN'
      AND ual.event_status = 'SUCCESS'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> loginVolumeByYear(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(ual.created_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_access_log ual
    LEFT JOIN user_access ua
        ON ua.access_id = ual.access_id
    WHERE ual.created_at >= :startDate
      AND ual.created_at <= :endDate
      AND ual.event_type = 'LOGIN'
      AND ual.event_status = 'SUCCESS'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> loginVolumeByMonth(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(ual.created_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_access_log ual
    LEFT JOIN user_access ua
        ON ua.access_id = ual.access_id
    WHERE ual.created_at >= :startDate
      AND ual.created_at <= :endDate
      AND ual.event_type = 'LOGIN'
      AND ual.event_status = 'SUCCESS'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> loginVolumeByDay(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(ual.created_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:00') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_access_log ual
    LEFT JOIN user_access ua
        ON ua.access_id = ual.access_id
    WHERE ual.created_at >= :startDate
      AND ual.created_at <= :endDate
      AND ual.event_type = 'LOGIN'
      AND ual.event_status = 'SUCCESS'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> loginVolumeByHour(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );
}