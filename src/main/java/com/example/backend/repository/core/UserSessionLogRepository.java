package com.example.backend.repository.core;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.dto.admin.monitoring.MetricCardDto;
import com.example.backend.entity.core.UserSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionLogRepository extends JpaRepository<UserSessionLog, Long> {

    Optional<UserSessionLog> findBySessionToken(String sessionToken);

    Optional<UserSessionLog> findFirstByUserAccess_AccessIdAndLoginStatusOrderByLoginAtDesc(
            Long accessId,
            String loginStatus
    );

    Optional<UserSessionLog> findFirstBySchoolIdOrderByLoginAtDesc(String schoolId);

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.MetricCardDto(
            'Active Sessions',
            COUNT(s),
            'Currently logged-in users with unexpired sessions'
        )
        FROM UserSessionLog s
        WHERE s.loginStatus = 'ACTIVE'
          AND s.logoutAt IS NULL
          AND s.expiresAt > :now
    """)
        MetricCardDto countActiveSessions(
                @Param("now") OffsetDateTime now
    );

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.MetricCardDto(
            'Session Volume',
            COUNT(s),
            'Total sessions within selected filters'
        )
        FROM UserSessionLog s
        WHERE s.loginAt >= :startDate
          AND s.loginAt <= :endDate
    """)
    MetricCardDto countSessionVolume(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );


    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
            'SESSION',
            COALESCE(l.loginAt, l.createdAt),
            l.logoutAt,
            l.schoolId,
            u.role,
            NULL,
            NULL,
            'AUTH',
            'SESSION',
            l.loginStatus,
            l.message,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL,
            l.ipAddress,
            l.deviceInfo,
            NULL
        )
        FROM UserSessionLog l
        JOIN UserAccess u ON u.schoolId = l.schoolId
        WHERE COALESCE(l.loginAt, l.createdAt) >= :startDate
          AND COALESCE(l.loginAt, l.createdAt) <= :endDate
          AND (
                :search = ''
                OR l.schoolId LIKE CONCAT('%', :search, '%')
                OR l.username LIKE CONCAT('%', :search, '%')
                OR l.loginStatus LIKE CONCAT('%', :search, '%')
                OR l.message LIKE CONCAT('%', :search, '%')
                OR l.ipAddress LIKE CONCAT('%', :search, '%')
                OR l.deviceInfo LIKE CONCAT('%', :search, '%')
              )
        ORDER BY COALESCE(l.loginAt, l.createdAt) DESC
    """)
        List<AdminLogRowDto> findSessionLogsForMonitoring(
                @Param("startDate") OffsetDateTime startDate,
                @Param("endDate") OffsetDateTime endDate,
                @Param("search") String search
        );


    // =======================
    // CONCURRENT USERS COUNT
    // =======================

    @Query(value = """
    SELECT
        to_char(usl.login_at AT TIME ZONE 'Asia/Manila', 'YYYY') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_session_log usl
    LEFT JOIN user_access ua
        ON ua.access_id = usl.access_id
    WHERE usl.login_at >= :startDate
      AND usl.login_at <= :endDate
      AND COALESCE(usl.login_status, '') = 'ACTIVE'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> concurrentUsersByYear(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(usl.login_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_session_log usl
    LEFT JOIN user_access ua
        ON ua.access_id = usl.access_id
    WHERE usl.login_at >= :startDate
      AND usl.login_at <= :endDate
      AND COALESCE(usl.login_status, '') = 'ACTIVE'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> concurrentUsersByMonth(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(usl.login_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_session_log usl
    LEFT JOIN user_access ua
        ON ua.access_id = usl.access_id
    WHERE usl.login_at >= :startDate
      AND usl.login_at <= :endDate
      AND COALESCE(usl.login_status, '') = 'ACTIVE'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> concurrentUsersByDay(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );

    @Query(value = """
    SELECT
        to_char(usl.login_at AT TIME ZONE 'Asia/Manila', 'YYYY-MM-DD HH24:00') AS label,
        COALESCE(ua.role, 'UNKNOWN') AS category,
        COUNT(*) AS value
    FROM user_session_log usl
    LEFT JOIN user_access ua
        ON ua.access_id = usl.access_id
    WHERE usl.login_at >= :startDate
      AND usl.login_at <= :endDate
      AND COALESCE(usl.login_status, '') = 'ACTIVE'
      AND (:role = 'All Roles' OR ua.role = :role)
    GROUP BY label, category
    ORDER BY label
""", nativeQuery = true)
    List<Object[]> concurrentUsersByHour(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("role") String role
    );


    @Query("""
    SELECT s
    FROM UserSessionLog s
    JOIN FETCH s.userAccess
    WHERE s.sessionToken = :token
""")
    Optional<UserSessionLog> findBySessionTokenWithUser(@Param("token") String token);

}