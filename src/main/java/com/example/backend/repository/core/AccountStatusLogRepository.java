package com.example.backend.repository.core;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.entity.core.AccountStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AccountStatusLogRepository extends JpaRepository<AccountStatusLog, Long> {

    List<AccountStatusLog> findBySchoolId(String schoolId);


    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
            'ACCOUNT',
            l.createdAt,
            null,
            l.performedBy,
            NULL,
            l.schoolId,
            l.role,
            'ACCOUNT',
            l.action,
            l.newStatus,
            l.reason,
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
        FROM AccountStatusLog l
        WHERE l.createdAt >= :startDate
          AND l.createdAt <= :endDate
          AND (
                :search = ''
                OR l.schoolId LIKE CONCAT('%', :search, '%')
                OR l.role LIKE CONCAT('%', :search, '%')
                OR l.action LIKE CONCAT('%', :search, '%')
                OR l.reason LIKE CONCAT('%', :search, '%')
                OR l.performedBy LIKE CONCAT('%', :search, '%')
                OR l.previousStatus LIKE CONCAT('%', :search, '%')
                OR l.newStatus LIKE CONCAT('%', :search, '%')
              )
        ORDER BY l.createdAt DESC
    """)
        List<AdminLogRowDto> findAccountLogsForMonitoring(
                @Param("startDate") OffsetDateTime startDate,
                @Param("endDate") OffsetDateTime endDate,
                @Param("search") String search
        );
}