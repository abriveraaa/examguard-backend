package com.example.backend.repository.exam;

import com.example.backend.dto.student.StudentResultHeaderDTO;
import com.example.backend.dto.student.dashboard.StudentResultSummaryDTO;
import com.example.backend.dto.student.dashboard.StudentUpcomingExamDTO;
import com.example.backend.entity.exam.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    @Query("""
    SELECT a.examId, COUNT(a)
    FROM ExamAttempt a
    WHERE a.examId IN :examIds
      AND a.status IN (
            com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
            com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
      )
    GROUP BY a.examId
""")
    List<Object[]> countSubmittedTakersByExamIds(
            @Param("examIds") List<Long> examIds
    );

    @Query("""
    SELECT COUNT(DISTINCT a.studentId)
    FROM ExamAttempt a
    WHERE a.examId = :examId
      AND a.status IN (
            com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
            com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
      )
""")
    Integer countSubmittedTakersByExamId(
            @Param("examId") Long examId
    );

    @Query(value = """
        SELECT COUNT(DISTINCT ce.student_id)
        FROM exam_assignment eca
        JOIN class_enrollment_cache ce
            ON ce.class_offering_id = eca.class_offering_id
        WHERE UPPER(ce.status) = 'ENROLLED'
          AND eca.exam_id = :examId
        """, nativeQuery = true)
    Integer countTakersByExamId(@Param("examId") Long examId);

    @Query("""
    SELECT ea.exam.examId, COUNT(DISTINCT ce.studentId)
    FROM ExamAssignment ea
    JOIN ClassEnrollmentCache ce
        ON ce.classOfferingId = ea.classOfferingId
    WHERE ea.exam.examId IN :examIds
      AND ce.status = 'ENROLLED'
    GROUP BY ea.exam.examId
""")
    List<Object[]> countTakersByExamIds(@Param("examIds") List<Long> examIds);

    @Query(value = """
    SELECT DISTINCT e.*
    FROM exam e
    JOIN exam_assignment ea ON ea.exam_id = e.exam_id
    JOIN faculty_load_cache fl ON fl.class_offering_id = ea.class_offering_id
    JOIN user_access ue ON ue.school_id = fl.employee_id
    WHERE ue.username = :employeeId

    UNION

    SELECT DISTINCT e.*
    FROM exam e
    WHERE e.created_by = :employeeId
    ORDER BY created_at DESC
    """, nativeQuery = true)
    List<Exam> findVisibleExamsForFaculty(@Param("employeeId") String employeeId);


    @Query(value = """
        SELECT DISTINCT e.*
        FROM exam e
        JOIN exam_assignment ea
            ON ea.exam_id = e.exam_id
        JOIN class_enrollment_cache ce
            ON ce.class_offering_id = ea.class_offering_id
        WHERE ce.student_id = :studentId
          AND ce.status = 'ENROLLED'
          AND CURRENT_TIMESTAMP BETWEEN e.start_datetime AND e.end_datetime
        ORDER BY e.start_datetime ASC
        """, nativeQuery = true)
    List<Exam> findAvailableExamsForStudent(@Param("studentId") String studentId);

    @Query("""
    SELECT new com.example.backend.dto.student.dashboard.StudentUpcomingExamDTO(
        e.examId,
        e.title,
        CONCAT(fp.firstName, ' ', fp.lastName),
        co.courseCode,
        co.courseDescription,
        e.startDateTime,
        e.endDateTime,
        e.timeLimitMinutes,
        e.examMode,
        e.status,
        COUNT(DISTINCT q.questionId),
        attempt.status
    )
    FROM Exam e
    JOIN ExamAssignment ass ON ass.exam.examId = e.examId
    JOIN ClassEnrollmentCache ce ON ce.classOfferingId = ass.classOfferingId
    JOIN ClassOfferingCache co ON co.classOfferingId = ass.classOfferingId
    JOIN FacultyLoadCache fl ON fl.classOfferingId = co.classOfferingId
    JOIN FacultyProfileCache fp ON fp.employeeId = fl.employeeId
    LEFT JOIN ExamQuestion q ON q.exam.examId = e.examId
    LEFT JOIN ExamAttempt attempt ON attempt.examId = e.examId AND attempt.studentId = :studentId
    WHERE ce.studentId = :studentId
      AND UPPER(ce.status) = 'ENROLLED'
      AND e.status = com.example.backend.entity.enums.ExamStatus.PUBLISHED
      AND e.endDateTime >= CURRENT_TIMESTAMP
      AND (
          attempt.status IS NULL
          OR attempt.status NOT IN (
              com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
              com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
          )
      )
    GROUP BY
        e.examId,
        e.title,
        fp.firstName,
        fp.lastName,
        co.courseCode,
        co.courseDescription,
        e.startDateTime,
        e.endDateTime,
        e.timeLimitMinutes,
        e.examMode,
        e.status,
        attempt.status
    ORDER BY e.startDateTime ASC
""")
    List<StudentUpcomingExamDTO> findPublishedAssignedExamsForStudent(
            @Param("studentId") String studentId
    );

    @Query("""
        SELECT new com.example.backend.dto.student.dashboard.StudentResultSummaryDTO(
            e.examId,
            e.title,
            co.courseCode,
            CASE
                WHEN e.resultsReleased = true
                    THEN 'RESULT_RELEASED'
                WHEN attempt.reviewStatus = 'REVIEWED'
                    THEN 'PENDING_RELEASE'
                ELSE 'FOR_CHECKING'
            END
        )
        FROM ExamAttempt attempt
        JOIN Exam e ON e.examId = attempt.examId
        JOIN ExamAssignment ass ON ass.exam.examId = e.examId
        JOIN ClassEnrollmentCache ce ON ce.classOfferingId = ass.classOfferingId
        JOIN ClassOfferingCache co ON co.classOfferingId = ass.classOfferingId
        WHERE attempt.studentId = :studentId
          AND ce.studentId = :studentId
          AND UPPER(ce.status) = 'ENROLLED'
          AND attempt.status IN (
              com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
              com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
          )
          AND (
              e.resultsReleased = false
              OR (
                  e.resultsReleased = true
                  AND e.resultsReleasedAt >= :cutoff
              )
          )
        ORDER BY attempt.submittedAt DESC
        """)
    List<StudentResultSummaryDTO> findSubmittedExamResultsForStudent(
            @Param("studentId") String studentId,
            @Param("cutoff") OffsetDateTime cutoff
    );

    @Query("""
    SELECT new com.example.backend.dto.student.dashboard.StudentResultSummaryDTO(
        e.examId,
        MIN(co.courseCode),
        e.title,
        'Results Released'
    )
    FROM ExamAttempt ea
    JOIN Exam e ON e.examId = ea.examId
    JOIN ClassEnrollmentCache ce ON ce.studentId = ea.studentId
    JOIN ClassOfferingCache co ON co.classOfferingId = ce.classOfferingId
    WHERE ea.studentId = :studentId
      AND ea.status IN (
          com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
          com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
      )
      AND e.resultsReleasedAt IS NOT NULL
      AND EXISTS (
          SELECT 1
          FROM ExamAssignment ass
          WHERE ass.exam.examId = e.examId
            AND ass.classOfferingId = ce.classOfferingId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM StudentDashboardView v
          WHERE v.studentId = :studentId
            AND v.itemType = 'RESULT'
            AND v.itemId = e.examId
      )
    GROUP BY e.examId, e.title, e.resultsReleasedAt
    ORDER BY e.resultsReleasedAt DESC
""")
    List<StudentResultSummaryDTO> findReleasedUnviewedResultsForStudent(
            @Param("studentId") String studentId,
            Pageable pageable
    );

    @Query("""
    SELECT new com.example.backend.dto.student.StudentResultHeaderDTO(
         MIN(co.courseCode),
         MIN(co.courseDescription),
         MIN(CONCAT(fp.firstName, ' ', fp.lastName)),
         MIN(co.term),
         MIN(co.academicYear)
     )
    FROM ExamAssignment ass
    JOIN ClassEnrollmentCache ce
        ON ce.classOfferingId = ass.classOfferingId
    JOIN ClassOfferingCache co
        ON co.classOfferingId = ass.classOfferingId
    LEFT JOIN FacultyLoadCache fl
        ON fl.classOfferingId = ass.classOfferingId
    LEFT JOIN FacultyProfileCache fp
        ON fp.employeeId = fl.employeeId
    WHERE ass.exam.examId = :examId
      AND ce.studentId = :studentId
      AND UPPER(ce.status) = 'ENROLLED'
""")
    Optional<StudentResultHeaderDTO> findStudentResultHeader(
            @Param("examId") Long examId,
            @Param("studentId") String studentId
    );

}