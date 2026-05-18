package com.example.backend.repository.core;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
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
                OR LOWER(COALESCE(l.schoolId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.loginStatus, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.message, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.ipAddress, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(l.deviceInfo, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
        ORDER BY COALESCE(l.loginAt, l.createdAt) DESC
    """)
        List<AdminLogRowDto> findSessionLogsForMonitoring(
                @Param("startDate") OffsetDateTime startDate,
                @Param("endDate") OffsetDateTime endDate,
                @Param("search") String search
        );

}