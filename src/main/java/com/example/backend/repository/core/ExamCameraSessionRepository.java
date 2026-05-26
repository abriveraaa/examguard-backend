package com.example.backend.repository.core;

import com.example.backend.dto.admin.monitoring.AdminLogRowDto;
import com.example.backend.entity.exam.ExamCameraSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamCameraSessionRepository extends JpaRepository<ExamCameraSession, Long> {

    Optional<ExamCameraSession> findByPairingToken(String pairingToken);

    Optional<ExamCameraSession> findTopByAttemptIdOrderByCreatedAtDesc(Long attemptId);

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.AdminLogRowDto(
            'CAMERA',
            COALESCE(c.startedAt, c.pairedAt, c.createdAt),
            c.endedAt,
            c.studentId,
            'STUDENT',
            NULL,
            NULL,
            'CAMERA_SESSION',
            c.deviceType,
            c.status,
            CONCAT(
                COALESCE(c.deviceLabel, 'Camera device'),
                ' • ',
                COALESCE(c.requiredAngle, '-'),
                ' • ',
                COALESCE(c.streamRole, '-'),
                ' • Disconnects: ',
                c.disconnectCount
            ),
            c.examId,
            c.attemptId,
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
        FROM ExamCameraSession c
        WHERE COALESCE(c.startedAt, c.pairedAt, c.createdAt) >= :startDate
          AND COALESCE(c.startedAt, c.pairedAt, c.createdAt) <= :endDate
          AND (
                :search = ''
                OR LOWER(COALESCE(c.studentId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(c.deviceLabel, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(c.deviceType, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(c.status, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(c.requiredAngle, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(c.streamRole, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
        ORDER BY COALESCE(c.startedAt, c.pairedAt, c.createdAt) DESC
    """)
    List<AdminLogRowDto> findCameraLogsForMonitoring(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("search") String search
    );
}