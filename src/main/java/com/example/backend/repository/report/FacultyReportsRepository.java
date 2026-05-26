package com.example.backend.repository.report;

import com.example.backend.dto.faculty.reports.*;
import com.example.backend.entity.exam.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacultyReportsRepository extends JpaRepository<Exam, Long> {

    @Query(value = """
    SELECT COALESCE(AVG(a.score_percentage), 0)
    FROM exam e
    JOIN exam_assignment ea
        ON ea.exam_id = e.exam_id
    JOIN class_offering_cache co
        ON co.class_offering_id = ea.class_offering_id
    JOIN faculty_load_cache fl
        ON fl.class_offering_id = co.class_offering_id
    JOIN class_enrollment_cache ce
        ON ce.class_offering_id = co.class_offering_id
    LEFT JOIN exam_attempt a
        ON a.exam_id = e.exam_id
       AND a.student_id = ce.student_id
    WHERE fl.employee_id = :facultyId
      AND co.academic_year = :academicYear
      AND co.term = :term
      AND (:courseCode IS NULL OR co.course_code = :courseCode)
      AND (:classOfferingId IS NULL OR co.class_offering_id = :classOfferingId)
      AND (:examId IS NULL OR e.exam_id = :examId)
      AND a.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
    """, nativeQuery = true)
    Double getAverageScore(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId,
            @Param("examId") Long examId
    );

    @Query(value = """
        SELECT COALESCE(
            (
                COUNT(DISTINCT CASE
                    WHEN a.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
                    THEN CONCAT(e.exam_id, '-', ce.student_id)
                    ELSE NULL
                END) * 100.0
            )
            /
            NULLIF(COUNT(DISTINCT CONCAT(e.exam_id, '-', ce.student_id)), 0),
            0
        )
        FROM exam e
        JOIN exam_assignment exa
            ON exa.exam_id = e.exam_id
        JOIN class_offering_cache co
            ON co.class_offering_id = exa.class_offering_id
        JOIN faculty_load_cache fl
            ON fl.class_offering_id = co.class_offering_id
        JOIN class_enrollment_cache ce
            ON ce.class_offering_id = co.class_offering_id
        LEFT JOIN exam_attempt a
            ON a.exam_id = e.exam_id
            AND a.student_id = ce.student_id
        WHERE fl.employee_id = :facultyId
        AND co.academic_year = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.course_code = :courseCode)
        AND (:classOfferingId IS NULL OR co.class_offering_id = :classOfferingId)
        AND (:examId IS NULL OR e.exam_id = :examId)
        """, nativeQuery = true)
    Double getSubmissionRate(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId,
            @Param("examId") Long examId
    );

    @Query(value = """
    SELECT COUNT(v.violation_id)
    FROM exam_violation_log v
    JOIN exam_attempt a
        ON a.attempt_id = v.attempt_id
    JOIN exam e
        ON e.exam_id = v.exam_id
    JOIN exam_assignment ea
        ON ea.exam_id = e.exam_id
    JOIN class_offering_cache co
        ON co.class_offering_id = ea.class_offering_id
    JOIN faculty_load_cache fl
        ON fl.class_offering_id = co.class_offering_id
    JOIN class_enrollment_cache ce
        ON ce.class_offering_id = co.class_offering_id
       AND ce.student_id = a.student_id
    WHERE fl.employee_id = :facultyId
      AND co.academic_year = :academicYear
      AND co.term = :term
      AND (:courseCode IS NULL OR co.course_code = :courseCode)
      AND (:classOfferingId IS NULL OR co.class_offering_id = :classOfferingId)
      AND (:examId IS NULL OR e.exam_id = :examId)
    """, nativeQuery = true)
    Long getTotalViolations(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId,
            @Param("examId") Long examId
    );

    @Query(value = """
    SELECT COUNT(v.violation_id)
    FROM exam_violation_log v
    JOIN exam_attempt a
        ON a.attempt_id = v.attempt_id
    JOIN exam e
        ON e.exam_id = v.exam_id
    JOIN exam_assignment ea
        ON ea.exam_id = e.exam_id
    JOIN class_offering_cache co
        ON co.class_offering_id = ea.class_offering_id
    JOIN faculty_load_cache fl
        ON fl.class_offering_id = co.class_offering_id
    JOIN class_enrollment_cache ce
        ON ce.class_offering_id = co.class_offering_id
       AND ce.student_id = a.student_id
    WHERE v.review_status = :reviewStatus
      AND fl.employee_id = :facultyId
      AND co.academic_year = :academicYear
      AND co.term = :term
      AND (:courseCode IS NULL OR co.course_code = :courseCode)
      AND (:classOfferingId IS NULL OR co.class_offering_id = :classOfferingId)
      AND (:examId IS NULL OR e.exam_id = :examId)
    """, nativeQuery = true)
    Long countViolationsByReviewStatus(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId,
            @Param("examId") Long examId,
            @Param("reviewStatus") String reviewStatus
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.reports.ExamParticipationDTO(
            e.examId,
            e.title,
        
            COALESCE(
                SUM(
                    CASE
                        WHEN a.status IN (
                            com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
                            com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
                        )
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ),
        
            COALESCE(
                AVG(a.scorePercentage),
                0
            )
        )
        
        FROM Exam e
        JOIN ExamAssignment ea
            ON ea.exam = e
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl
            ON fl.classOfferingId = co.classOfferingId
        
        JOIN ClassEnrollmentCache ce
            ON ce.classOfferingId = co.classOfferingId
        
        LEFT JOIN ExamAttempt a
            ON a.examId = e.examId
            AND a.studentId = ce.studentId
        
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.courseCode = :courseCode)
        AND (:classOfferingId IS NULL OR co.classOfferingId = :classOfferingId)
        
        GROUP BY
            e.examId,
            e.title
        
        ORDER BY e.examId ASC
        """)
    List<ExamParticipationDTO> getParticipation(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId
    );

    @Query(value = """
        WITH assigned_students AS (
            SELECT DISTINCT
                e.exam_id,
                ce.student_id
            FROM exam e
            JOIN exam_assignment exa
                ON exa.exam_id = e.exam_id
            JOIN class_offering_cache co
                ON co.class_offering_id = exa.class_offering_id
            JOIN faculty_load_cache fl
                ON fl.class_offering_id = co.class_offering_id
            JOIN class_enrollment_cache ce
                ON ce.class_offering_id = co.class_offering_id
            WHERE fl.employee_id = :facultyId
            AND co.academic_year = :academicYear
            AND co.term = :term
            AND (:courseCode IS NULL OR co.course_code = :courseCode)
            AND (:classOfferingId IS NULL OR co.class_offering_id = :classOfferingId)
            AND (:examId IS NULL OR e.exam_id = :examId)
        ),
        
        student_status AS (
            SELECT
                s.exam_id,
                s.student_id,
                CASE
                    WHEN a.attempt_id IS NULL THEN 'DID_NOT_TAKE'
                    WHEN a.status IN ('SUBMITTED', 'AUTO_SUBMITTED') THEN 'SUBMITTED'
                    WHEN a.status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
                    ELSE a.status
                END AS status
            FROM assigned_students s
            LEFT JOIN exam_attempt a
                ON a.exam_id = s.exam_id
                AND a.student_id = s.student_id
        )
        
        SELECT
            status AS status,
            COUNT(*) AS count
        FROM student_status
        GROUP BY status
        ORDER BY status
        """, nativeQuery = true)
    List<SubmissionStatusProjection> getSubmissionStatusRaw(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId,
            @Param("examId") Long examId
    );

    @Query(value = """
        WITH assigned_students AS (
            SELECT DISTINCT
                e.exam_id,
                e.title AS exam_title,
                ce.student_id
            FROM exam e
            JOIN exam_assignment ea
                ON ea.exam_id = e.exam_id
            JOIN class_offering_cache co
                ON co.class_offering_id = ea.class_offering_id
            JOIN faculty_load_cache fl
                ON fl.class_offering_id = co.class_offering_id
            JOIN class_enrollment_cache ce
                ON ce.class_offering_id = co.class_offering_id
            WHERE fl.employee_id = :facultyId
              AND co.academic_year = :academicYear
              AND co.term = :term
              AND (:courseCode IS NULL OR co.course_code = :courseCode)
              AND (:classOfferingId IS NULL OR co.class_offering_id = :classOfferingId)
        ),
    
        student_status AS (
            SELECT
                s.exam_id,
                s.exam_title,
                s.student_id,
                CASE
                    WHEN a.attempt_id IS NULL THEN 'DID_NOT_TAKE'
                    WHEN a.status IN ('SUBMITTED', 'AUTO_SUBMITTED') THEN 'SUBMITTED'
                    WHEN a.status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
                    ELSE CAST(a.status AS TEXT)
                END AS status
            FROM assigned_students s
            LEFT JOIN exam_attempt a
                ON a.exam_id = s.exam_id
               AND a.student_id = s.student_id
        )
    
        SELECT
            exam_id AS examId,
            exam_title AS examTitle,
            status AS status,
            COUNT(*) AS count
        FROM student_status
        GROUP BY
            exam_id,
            exam_title,
            status
        ORDER BY
            exam_id ASC,
            status ASC
        """, nativeQuery = true)
    List<Object[]> getSubmissionBreakdownRaw(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.reports.ViolationTypeDTO(
            e.examId,
            e.title,
            v.violationType,
            COUNT(v.violationId)
        )
        FROM ExamViolationLog v
        JOIN ExamAttempt a
            ON a.attemptId = v.attempt.attemptId
        JOIN Exam e
            ON e.examId = v.exam.examId
        JOIN ExamAssignment ea
            ON ea.exam = e
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl
            ON fl.classOfferingId = co.classOfferingId
        JOIN ClassEnrollmentCache ce
            ON ce.classOfferingId = co.classOfferingId
            AND ce.studentId = a.studentId
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.courseCode = :courseCode)
        AND (:classOfferingId IS NULL OR co.classOfferingId = :classOfferingId)
        AND (:examId IS NULL OR e.examId = :examId)
        GROUP BY e.examId, e.title, v.violationType
        ORDER BY COUNT(v.violationId) DESC
        """)
    List<ViolationTypeDTO> getViolations(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId,
            @Param("examId") Long examId
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.reports.ReportExamOptionDTO(
            e.examId,
            e.title,
            COUNT(DISTINCT ea.classOfferingId)
        )
        FROM Exam e
        JOIN ExamAssignment ea
            ON ea.exam = e
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl
            ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.courseCode = :courseCode)
        AND (:classOfferingId IS NULL OR co.classOfferingId = :classOfferingId)
        
        GROUP BY
            e.examId,
            e.title
        
        ORDER BY e.examId DESC
        """)
    List<ReportExamOptionDTO> getExamOptions(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    );

    // =====================
    // CLASS RECORD
    // =====================

    @Query(value = """
    SELECT
        e.exam_id AS examId,
        e.title AS examTitle,
        COALESCE(SUM(eq.points),0) AS totalPoints,
        e.start_datetime AS startDateTime,
        e.end_datetime AS endDateTime
    FROM exam e
    JOIN exam_assignment ea ON ea.exam_id = e.exam_id
    JOIN class_offering_cache co ON co.class_offering_id = ea.class_offering_id
    JOIN faculty_load_cache fl ON fl.class_offering_id = co.class_offering_id
    LEFT JOIN exam_question eq ON eq.exam_id = e.exam_id
    WHERE fl.employee_id = :facultyId AND co.class_offering_id = :classOfferingId
    GROUP BY
        e.exam_id,
        e.title,
        e.start_datetime,
        e.end_datetime
    ORDER BY
        e.start_datetime ASC,
        e.exam_id ASC
    """, nativeQuery = true)
    List<Object[]> findClassRecordExamColumns(
            @Param("facultyId") String facultyId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query(value = """
        SELECT DISTINCT
            ce.student_id AS studentId,
            TRIM(
                COALESCE(sp.first_name, '') || ' ' || COALESCE(sp.last_name, '')
            ) AS studentName,
            co.section_name AS sectionName
        FROM class_enrollment_cache ce
        JOIN class_offering_cache co
            ON co.class_offering_id = ce.class_offering_id
        JOIN faculty_load_cache fl
            ON fl.class_offering_id = co.class_offering_id
        LEFT JOIN student_profile_cache sp
            ON sp.student_id = ce.student_id
        WHERE fl.employee_id = :facultyId
          AND co.class_offering_id = :classOfferingId
        ORDER BY studentName ASC
        """, nativeQuery = true)
    List<Object[]> findClassRecordStudents(
            @Param("facultyId") String facultyId,
            @Param("classOfferingId") String classOfferingId
    );


    @Query(value = """
        SELECT DISTINCT
            ce.student_id AS studentId,
            e.exam_id AS examId,

            CASE
                WHEN a.attempt_id IS NULL THEN 0
                WHEN a.status = 'IN_PROGRESS' AND a.total_score IS NULL THEN NULL
                ELSE a.total_score
            END AS score,

            COALESCE(t.total_points, 0) AS totalPoints,

            CASE
                WHEN a.attempt_id IS NULL THEN 0
                WHEN a.status = 'IN_PROGRESS' AND a.total_score IS NULL THEN NULL
                WHEN t.total_points IS NULL OR t.total_points = 0 THEN NULL
                WHEN a.total_score IS NULL THEN NULL
                ELSE ROUND(
                    (
                        a.total_score::numeric
                        /
                        t.total_points::numeric
                    ) * 100,
                    2
                )
            END AS percentage,

            CASE
                WHEN a.attempt_id IS NULL THEN 'DID_NOT_TAKE'
                WHEN a.status = 'IN_PROGRESS' AND a.total_score IS NULL THEN 'PENDING'
                ELSE CAST(a.status AS TEXT)
            END AS status

        FROM class_enrollment_cache ce
        JOIN class_offering_cache co
            ON co.class_offering_id = ce.class_offering_id
        JOIN faculty_load_cache fl
            ON fl.class_offering_id = co.class_offering_id
        JOIN exam_assignment ea
            ON ea.class_offering_id = co.class_offering_id
        JOIN exam e
            ON e.exam_id = ea.exam_id
        LEFT JOIN exam_attempt a
            ON a.exam_id = e.exam_id
           AND a.student_id = ce.student_id
        LEFT JOIN (
            SELECT
                exam_id,
                SUM(points) AS total_points
            FROM exam_question
            GROUP BY exam_id
        ) t
            ON t.exam_id = e.exam_id
        WHERE fl.employee_id = :facultyId
          AND co.class_offering_id = :classOfferingId
          AND e.exam_id IN (:examIds)
        ORDER BY
            ce.student_id ASC,
            e.exam_id ASC
        """, nativeQuery = true)
    List<Object[]> findClassRecordScores(
            @Param("facultyId") String facultyId,
            @Param("classOfferingId") String classOfferingId,
            @Param("examIds") List<Long> examIds
    );

    // =====================
    // COLLEGE OFFERING
    // =====================

    @Query(value = """
        SELECT
            co.course_code AS courseCode,
            co.course_description AS courseDescription,
            CONCAT( co.program_code, ' ', co.year_level, '-', co.section_name ) AS sectionLabel,
            co.college_offering AS collegeOffering
        FROM class_offering_cache co
        JOIN faculty_load_cache fl
            ON fl.class_offering_id = co.class_offering_id
        WHERE fl.employee_id = :facultyId
          AND co.class_offering_id = :classOfferingId
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> findClassRecordSectionInfo(
            @Param("facultyId") String facultyId,
            @Param("classOfferingId") String classOfferingId
    );


}