package com.example.backend.repository.report;

import com.example.backend.dto.faculty.reports.*;
import com.example.backend.entity.exam.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacultyReportsRepository extends JpaRepository<Exam, Long> {

    @Query("""
    SELECT COALESCE(AVG(a.scorePercentage), 0)
    FROM ExamAttempt a
    WHERE a.examId IN (
        SELECT DISTINCT e.examId
        FROM Exam e
        JOIN ExamAssignment ea ON ea.exam = e
        JOIN ClassOfferingCache co ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.courseCode = :courseCode)
        AND (:classOfferingId IS NULL OR co.classOfferingId = :classOfferingId)
        AND (:examId IS NULL OR e.examId = :examId)
    )
    """)
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

    @Query("""
    SELECT COUNT(v.violationId)
    FROM ExamViolationLog v
    WHERE v.exam.examId IN (
        SELECT DISTINCT e.examId
        FROM Exam e
        JOIN ExamAssignment ea ON ea.exam = e
        JOIN ClassOfferingCache co ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.courseCode = :courseCode)
        AND (:classOfferingId IS NULL OR co.classOfferingId = :classOfferingId)
        AND (:examId IS NULL OR e.examId = :examId)
    )
    """)
    Long getTotalViolations(
            @Param("facultyId") String facultyId,
            @Param("academicYear") String academicYear,
            @Param("term") String term,
            @Param("courseCode") String courseCode,
            @Param("classOfferingId") String classOfferingId,
            @Param("examId") Long examId
    );

    @Query("""
    SELECT COUNT(v.violationId)
    FROM ExamViolationLog v
    WHERE v.reviewStatus = :reviewStatus
    AND v.exam.examId IN (
        SELECT DISTINCT e.examId
        FROM Exam e
        JOIN ExamAssignment ea ON ea.exam = e
        JOIN ClassOfferingCache co ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.courseCode = :courseCode)
        AND (:classOfferingId IS NULL OR co.classOfferingId = :classOfferingId)
        AND (:examId IS NULL OR e.examId = :examId)
    )
    """)
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
        
            COALESCE(AVG(a.scorePercentage), 0)
        )
        FROM Exam e
        JOIN ExamAssignment ea
            ON ea.exam = e
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl
            ON fl.classOfferingId = co.classOfferingId
        LEFT JOIN ExamAttempt a
            ON a.examId = e.examId
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (
            :courseCode IS NULL
            OR co.courseCode = :courseCode
        )
        AND (
            :classOfferingId IS NULL
            OR co.classOfferingId = :classOfferingId
        )
        GROUP BY e.examId, e.title
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

    @Query("""
    SELECT new com.example.backend.dto.faculty.reports.ViolationTypeDTO(
        v.violationType,
        COUNT(v.violationId)
    )
    FROM ExamViolationLog v
    WHERE v.exam.examId IN (
        SELECT DISTINCT e.examId
        FROM Exam e
        JOIN ExamAssignment ea ON ea.exam = e
        JOIN ClassOfferingCache co ON co.classOfferingId = ea.classOfferingId
        JOIN FacultyLoadCache fl ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :facultyId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND (:courseCode IS NULL OR co.courseCode = :courseCode)
        AND (:classOfferingId IS NULL OR co.classOfferingId = :classOfferingId)
        AND (:examId IS NULL OR e.examId = :examId)
    )
    GROUP BY v.violationType
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
}