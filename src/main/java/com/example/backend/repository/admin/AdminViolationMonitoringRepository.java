package com.example.backend.repository.admin;

import com.example.backend.dto.admin.monitoring.MetricCardDto;
import com.example.backend.entity.exam.ExamViolationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AdminViolationMonitoringRepository extends JpaRepository<ExamViolationLog, Long> {

    @Query("""
        SELECT new com.example.backend.dto.admin.monitoring.MetricCardDto(
            'Total Violations',
            COUNT(v),
            'Violation logs within selected filters'
        )
        FROM ExamViolationLog v
        WHERE (v.occurredAt >= :startDate)
          AND (v.occurredAt <= :endDate)
    """)
    MetricCardDto countViolations(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
    SELECT
        COALESCE(v.violationType, 'UNKNOWN'),
        COALESCE(v.severity, 'UNKNOWN'),
        COUNT(v)
    FROM ExamViolationLog v
    WHERE (v.occurredAt >= :startDate)
      AND (v.occurredAt <= :endDate)
    GROUP BY
        COALESCE(v.violationType, 'UNKNOWN'),
        COALESCE(v.severity, 'UNKNOWN')
    ORDER BY COUNT(v) DESC
""")
    List<Object[]> violationsByTypeRaw(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
    SELECT
        COALESCE(s.programCode, 'UNKNOWN'),
        COALESCE(s.programName, 'Unknown Program'),
        COUNT(v)
    FROM ExamViolationLog v
    JOIN v.attempt a
    JOIN StudentProfileCache s ON s.studentId = a.studentId
    WHERE (v.occurredAt >= :startDate)
      AND (v.occurredAt <= :endDate)
      AND (:programCode IS NULL OR :programCode = 'All Programs' OR s.programCode = :programCode)
      AND (:collegeCode IS NULL OR :collegeCode = 'All Colleges' OR s.collegeCode = :collegeCode)
    GROUP BY
        COALESCE(s.programCode, 'UNKNOWN'),
        COALESCE(s.programName, 'Unknown Program')
    ORDER BY COUNT(v) DESC
""")
    List<Object[]> violationsByProgramRaw(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("programCode") String programCode,
            @Param("collegeCode") String collegeCode
    );

    @Query(value = """
    SELECT
        'VIOLATION' AS source,
        v.occurred_at AS occurred_at,
        null,
        a.student_id AS actor_id,
        'STUDENT' AS actor_role,
        NULL AS target_user_id,
        NULL AS target_role,
        'EXAM' AS module,
        v.violation_type AS action,
        v.review_status AS status,
        v.violation_message AS message,
        v.exam_id AS exam_id,
        a.attempt_id AS attempt_id,
        v.question_id AS question_id,
        NULL AS duration_ms,
        s.program_code AS program_code,
        s.program_name AS program_name,
        s.section_name AS section,
        v.severity AS severity,
        COALESCE(
            STRING_AGG(DISTINCT c.course_code, ', '),
            '-'
        ) AS course_code,
        ex.title AS exam_title,
        q.question_order AS question_number
    FROM exam_violation_log v
    JOIN exam_attempt a
        ON a.attempt_id = v.attempt_id
    JOIN exam ex
        ON ex.exam_id = v.exam_id
    LEFT JOIN exam_question q
        ON q.question_id = v.question_id
    LEFT JOIN student_profile_cache s
        ON s.student_id = a.student_id
    LEFT JOIN exam_assignment ea
        ON ea.exam_id = v.exam_id
    LEFT JOIN class_offering_cache c
        ON c.class_offering_id = ea.class_offering_id
    WHERE v.occurred_at >= :startDate
      AND v.occurred_at <= :endDate
      AND (:severity = 'All Severities' OR v.severity = :severity)
      AND (
            :search = ''
            OR LOWER(COALESCE(a.student_id, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(v.violation_type, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(v.severity, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(v.review_status, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(v.violation_message, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(s.program_code, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(s.program_name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(c.course_code, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(ex.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    GROUP BY
        v.violation_id,
        v.occurred_at,
        a.student_id,
        v.violation_type,
        v.review_status,
        v.violation_message,
        v.exam_id,
        a.attempt_id,
        v.question_id,
        s.program_code,
        s.program_name,
        s.section_name,
        v.severity,
        ex.title,
        q.question_order
    ORDER BY v.occurred_at DESC
""", nativeQuery = true)
    List<Object[]> findViolationLogsForMonitoringRaw(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("severity") String severity,
            @Param("search") String search
    );
}