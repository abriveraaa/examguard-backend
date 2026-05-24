package com.example.backend.repository.report;

import com.example.backend.entity.exam.Exam;
import com.example.backend.report.exam.dto.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends Repository<Exam, Long> {

    @Query("""
        SELECT new com.example.backend.report.exam.dto.ReportExamHeaderDTO(
            e.examId,
            e.title,
            MIN(co.courseCode),
            MIN(co.courseDescription),
            CONCAT(fp.firstName, ' ', fp.lastName),
            MIN(co.collegeOffering),
            e.timeLimitMinutes,
            e.startDateTime,
            e.endDateTime,
            COALESCE((
                SELECT SUM(q.points)
                FROM ExamQuestion q
                WHERE q.exam.examId = e.examId
            ), 0)
        )
        FROM Exam e
        JOIN ExamAssignment ass 
            ON ass.exam.examId = e.examId
        JOIN ClassOfferingCache co 
            ON co.classOfferingId = ass.classOfferingId
        JOIN FacultyLoadCache fl
            ON fl.classOfferingId = co.classOfferingId
        JOIN FacultyProfileCache fp
            ON fp.employeeId = fl.employeeId
        WHERE e.examId = :examId
          AND (:classOfferingId IS NULL OR ass.classOfferingId = :classOfferingId)
        GROUP BY 
            e.examId,
            e.title,
            CONCAT(fp.firstName, ' ', fp.lastName),
            e.timeLimitMinutes,
            e.startDateTime,
            e.endDateTime
        """)
    ReportExamHeaderDTO findExamHeaderForReport(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT DISTINCT 
    CONCAT(
        co.programCode,
        ' ',
        co.yearLevel,
        '-',
        co.sectionName
    )
    FROM ExamAssignment ass
    JOIN ClassOfferingCache co ON co.classOfferingId = ass.classOfferingId
    WHERE ass.exam.examId = :examId
      AND (:classOfferingId IS NULL OR ass.classOfferingId = :classOfferingId)
""")
    List<String> findAssignedSectionsForReport(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT new com.example.backend.report.exam.dto.ReportQuestionDTO(
        q.questionId,
        q.questionOrder,
        q.questionType,
        q.questionText,
        q.questionInstruction,
        q.correctAnswer,
        q.points
    )
    FROM ExamQuestion q
    WHERE q.exam.examId = :examId
    ORDER BY q.questionType ASC, q.questionOrder ASC
""")
    List<ReportQuestionDTO> findQuestionsForReport(
            @Param("examId") Long examId
    );

    @Query("""
        SELECT new com.example.backend.report.exam.dto.ReportChoiceDTO(
            c.question.questionId,
            c.choiceId,
            c.choiceLabel,
            c.choiceText,
            c.correct
        )
        FROM ExamChoice c
        WHERE c.question.exam.examId = :examId
        ORDER BY c.question.questionOrder ASC, c.choiceOrder ASC
    """)
    List<ReportChoiceDTO> findChoicesForReport(
            @Param("examId") Long examId
    );

    @Query("""
    SELECT new com.example.backend.report.exam.dto.ReportRubricDTO(
        r.question.questionId,
        r.criterionName,
        r.weightPercentage
    )
    FROM EssayRubric r
    WHERE r.question.exam.examId = :examId
    ORDER BY r.question.questionOrder ASC, r.displayOrder ASC
""")
    List<ReportRubricDTO> findRubricsForReport(
            @Param("examId") Long examId
    );

    @Query("""
    SELECT new com.example.backend.report.exam.dto.ReportStudentSummaryDTO(
        a.attemptId,
        s.studentId,
        CONCAT(s.firstName, ' ', s.lastName),
        CONCAT(
            s.programCode,
            ' ',
            s.yearLevel,
            CASE
                WHEN s.sectionName IS NOT NULL
                     AND TRIM(s.sectionName) <> ''
                THEN CONCAT('-', s.sectionName)
                ELSE ''
            END
        ),
        CONCAT(fp.firstName, ' ', fp.lastName),
        COALESCE(a.totalScore, 0.0),
        COALESCE(a.scorePercentage, 0.0),
        CASE
            WHEN a.status IS NULL THEN 'DID_NOT_TAKE'
            ELSE CAST(a.status AS string)
        END,
        a.startedAt,
        a.submittedAt
    )
    FROM ExamAssignment ass
    JOIN ClassEnrollmentCache ce
        ON ce.classOfferingId = ass.classOfferingId
        AND UPPER(ce.status) = 'ENROLLED'
    JOIN StudentProfileCache s
        ON s.studentId = ce.studentId
    JOIN FacultyLoadCache fl
        ON fl.classOfferingId = ce.classOfferingId
    JOIN FacultyProfileCache fp
        ON fp.employeeId = fl.employeeId
    LEFT JOIN ExamAttempt a
        ON a.examId = ass.exam.examId
        AND a.studentId = s.studentId
    WHERE ass.exam.examId = :examId
        AND (:classOfferingId IS NULL OR ass.classOfferingId = :classOfferingId)
    ORDER BY
        COALESCE(a.scorePercentage, 0.0) DESC,
        s.lastName ASC,
        s.firstName ASC
""")
    List<ReportStudentSummaryDTO> findStudentSummariesForReport(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT DISTINCT new com.example.backend.report.exam.dto.ReportStudentViolationDTO(
        a.studentId,
        v.violationType
    )
    FROM ExamViolationLog v
    JOIN v.attempt a
    JOIN ClassEnrollmentCache ce
        ON ce.studentId = a.studentId
    JOIN ExamAssignment ass
        ON ass.exam.examId = a.examId
        AND ass.classOfferingId = ce.classOfferingId
    WHERE a.examId = :examId
      AND UPPER(ce.status) = 'ENROLLED'
      AND (:classOfferingId IS NULL OR ass.classOfferingId = :classOfferingId)
    ORDER BY a.studentId ASC
""")
    List<ReportStudentViolationDTO> findStudentViolationsForReport(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT new com.example.backend.report.exam.dto.ReportStudentDurationDTO(
        l.attemptId,
        COALESCE(SUM(l.durationMs), 0)
    )
    FROM SystemActivityLog l
    JOIN ExamAttempt a
        ON a.attemptId = l.attemptId
    JOIN ClassEnrollmentCache ce
        ON ce.studentId = a.studentId
    JOIN ExamAssignment ass
        ON ass.exam.examId = a.examId
        AND ass.classOfferingId = ce.classOfferingId
    WHERE l.examId = :examId
      AND l.attemptId IS NOT NULL
      AND l.durationMs IS NOT NULL
      AND l.module = 'EXAM_TAKING'
      AND l.action IN ('QUESTION_DURATION')
      AND UPPER(ce.status) = 'ENROLLED'
      AND (:classOfferingId IS NULL OR ass.classOfferingId = :classOfferingId)
    GROUP BY l.attemptId
""")
    List<ReportStudentDurationDTO> findStudentDurationsForReport(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT new com.example.backend.report.exam.dto.ReportStudentAnswerDTO(
        a.attemptId,
        ans.answerId,
        s.studentId,
        CONCAT(s.firstName, ' ', s.lastName),
        q.questionId,
        q.questionOrder,
        q.questionType,
        q.questionText,
        q.questionInstruction,
        q.correctAnswer,
        sc.choiceId,
        sc.choiceText,
        ans.answerText,
        ans.isCorrect,
        CAST(ans.pointsAwarded AS double),
        CAST(q.points AS double),
        ans.facultyFeedback
    )
    FROM ExamAttempt a
    JOIN StudentProfileCache s
        ON s.studentId = a.studentId
    JOIN ClassEnrollmentCache ce
        ON ce.studentId = a.studentId
    JOIN ExamAssignment ass
        ON ass.exam.examId = a.examId
        AND ass.classOfferingId = ce.classOfferingId
    JOIN ExamQuestion q
        ON q.exam.examId = a.examId
    LEFT JOIN ExamAnswer ans
        ON ans.attempt.attemptId = a.attemptId
        AND ans.question.questionId = q.questionId
    LEFT JOIN ans.selectedChoiceId sc
    WHERE a.examId = :examId
        AND UPPER(ce.status) = 'ENROLLED'
        AND (:classOfferingId IS NULL OR ass.classOfferingId = :classOfferingId)
    ORDER BY
        s.lastName ASC,
        s.firstName ASC,
        q.questionType ASC,
        q.questionOrder ASC
""")
    List<ReportStudentAnswerDTO> findStudentAnswersForReport(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT new com.example.backend.report.exam.dto.ReportEssayRubricScoreDTO(
        ans.attempt.attemptId,
        ans.question.questionId,
        rs.rubric.criterionName,
        CAST(ans.question.points AS double),
        CAST(rs.rubric.weightPercentage AS double),
        CAST(rs.scoreAwarded AS double),
        CAST(rs.scorePercentage AS double),
        rs.feedback
    )
    FROM EssayRubricScore rs
    JOIN rs.answer ans
    JOIN ClassEnrollmentCache ce
        ON ce.studentId = ans.attempt.studentId
    JOIN ExamAssignment ass
        ON ass.exam.examId = ans.attempt.examId
        AND ass.classOfferingId = ce.classOfferingId
    WHERE ans.attempt.examId = :examId
        AND UPPER(ce.status) = 'ENROLLED'
        AND (:classOfferingId IS NULL OR ass.classOfferingId = :classOfferingId)
    ORDER BY
        ans.attempt.attemptId ASC,
        ans.question.questionOrder ASC,
        rs.rubric.displayOrder ASC
""")
    List<ReportEssayRubricScoreDTO> findEssayRubricScoresForReport(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT ass.classOfferingId
    FROM ExamAssignment ass
    WHERE ass.exam.examId = :examId
    ORDER BY ass.classOfferingId ASC
""")
    List<String> findAssignedClassOfferingIdsForReport(
            @Param("examId") Long examId
    );

    @Query("""
    SELECT ass.classOfferingId
    FROM ExamAssignment ass
    JOIN ClassEnrollmentCache ce
        ON ce.classOfferingId = ass.classOfferingId
    WHERE ass.exam.examId = :examId
      AND ce.studentId = :studentId
""")
    String findStudentClassOfferingIdForExam(
            @Param("examId") Long examId,
            @Param("studentId") String studentId
    );

    @Query(value = """
        WITH assigned AS (
            SELECT DISTINCT ce.student_id
            FROM exam_assignment ea
            JOIN class_enrollment_cache ce ON ce.class_offering_id = ea.class_offering_id AND UPPER(ce.status) = 'ENROLLED'
            WHERE ea.exam_id = :examId
              AND (:classOfferingId IS NULL OR ea.class_offering_id = :classOfferingId)
        ),
        attempts AS (
            SELECT
                a.student_id,
                a.attempt_id,
                a.status,
                a.total_score,
                a.score_percentage
            FROM exam_attempt a
            WHERE a.exam_id = :examId
        ),
        joined_data AS (
            SELECT
                ass.student_id,
                at.attempt_id,
                COALESCE(CAST(at.status AS TEXT), 'DID_NOT_TAKE') AS attempt_status,
                at.total_score,
                at.score_percentage
            FROM assigned ass
            LEFT JOIN attempts at
                ON at.student_id = ass.student_id
        ),
        violation_students AS (
            SELECT DISTINCT a.student_id
            FROM exam_violation_log v
            JOIN exam_attempt a
                ON a.attempt_id = v.attempt_id
            JOIN exam_assignment ea
                ON ea.exam_id = v.exam_id
            JOIN class_enrollment_cache ce
                ON ce.class_offering_id = ea.class_offering_id
               AND ce.student_id = a.student_id
               AND UPPER(ce.status) = 'ENROLLED'
            WHERE v.exam_id = :examId
              AND (:classOfferingId IS NULL OR ea.class_offering_id = :classOfferingId)
            )
        SELECT
            COUNT(*) AS assigned_students,

            SUM(CASE WHEN attempt_status = 'SUBMITTED' THEN 1 ELSE 0 END) AS submitted,

            SUM(CASE WHEN attempt_status = 'AUTO_SUBMITTED' THEN 1 ELSE 0 END) AS auto_submitted,

            SUM(CASE WHEN attempt_status = 'DID_NOT_TAKE' THEN 1 ELSE 0 END) AS did_not_take,

            COALESCE(
                (
                    SUM(
                        CASE
                            WHEN attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
                            THEN 1 ELSE 0
                        END
                    ) * 100.0
                ) / NULLIF(COUNT(*), 0),
                0
            ) AS submission_rate,

            COALESCE(AVG(total_score) FILTER (
                WHERE attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            ), 0) AS average_score,

            COALESCE(AVG(score_percentage) FILTER (
                WHERE attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            ), 0) AS average_percentage,

            COALESCE(MAX(total_score) FILTER (
                WHERE attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            ), 0) AS highest_score,

            COALESCE(MAX(score_percentage) FILTER (
                WHERE attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            ), 0) AS highest_percentage,

            COALESCE(MIN(total_score) FILTER (
                WHERE attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            ), 0) AS lowest_score,

            COALESCE(MIN(score_percentage) FILTER (
                WHERE attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            ), 0) AS lowest_percentage,

            COALESCE(
                (
                    SUM(
                        CASE
                            WHEN score_percentage >= 60
                             AND attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
                            THEN 1 ELSE 0
                        END
                    ) * 100.0
                )
                / NULLIF(
                    SUM(
                        CASE
                            WHEN attempt_status IN ('SUBMITTED', 'AUTO_SUBMITTED')
                            THEN 1 ELSE 0
                        END
                    ),
                    0
                ),
                0
            ) AS passing_rate,

            COUNT(DISTINCT vs.student_id) AS with_violations

        FROM joined_data jd
        LEFT JOIN violation_students vs
            ON vs.student_id = jd.student_id
        """, nativeQuery = true)
    Object[] findExamResultSummaryMetricsRaw(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );


    @Query(value = """
    WITH submitted_attempts AS (
        SELECT
            a.score_percentage
        FROM exam_attempt a
        JOIN exam_assignment ea
            ON ea.exam_id = a.exam_id
        JOIN class_enrollment_cache ce
            ON ce.class_offering_id = ea.class_offering_id
           AND ce.student_id = a.student_id
           AND UPPER(ce.status) = 'ENROLLED'
        WHERE a.exam_id = :examId
          AND a.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
          AND (:classOfferingId IS NULL OR ea.class_offering_id = :classOfferingId)
    )
    SELECT '0-20%' AS range_label,
           COUNT(*) FILTER (WHERE score_percentage >= 0 AND score_percentage <= 20) AS student_count
    FROM submitted_attempts

    UNION ALL

    SELECT '21-40%' AS range_label,
           COUNT(*) FILTER (WHERE score_percentage > 20 AND score_percentage <= 40) AS student_count
    FROM submitted_attempts

    UNION ALL

    SELECT '41-60%' AS range_label,
           COUNT(*) FILTER (WHERE score_percentage > 40 AND score_percentage <= 60) AS student_count
    FROM submitted_attempts

    UNION ALL

    SELECT '61-80%' AS range_label,
           COUNT(*) FILTER (WHERE score_percentage > 60 AND score_percentage <= 80) AS student_count
    FROM submitted_attempts

    UNION ALL

    SELECT '81-100%' AS range_label,
           COUNT(*) FILTER (WHERE score_percentage > 80 AND score_percentage <= 100) AS student_count
    FROM submitted_attempts
    """, nativeQuery = true)
    List<Object[]> findScoreDistributionRaw(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );


    @Query(value = """
        SELECT
            CAST(v.violation_type AS TEXT) AS violation_type,
            COUNT(*) AS violation_count,
            COUNT(DISTINCT a.student_id) AS affected_students
        FROM exam_violation_log v
        JOIN exam_attempt a
            ON a.attempt_id = v.attempt_id
        JOIN exam_assignment ea
            ON ea.exam_id = v.exam_id
        JOIN class_enrollment_cache ce
            ON ce.class_offering_id = ea.class_offering_id
           AND ce.student_id = a.student_id
           AND UPPER(ce.status) = 'ENROLLED'
        WHERE v.exam_id = :examId
          AND (:classOfferingId IS NULL OR ea.class_offering_id = :classOfferingId)
        GROUP BY CAST(v.violation_type AS TEXT)
        ORDER BY violation_count DESC
        """, nativeQuery = true)
    List<Object[]> findViolationSummaryRaw(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );

    @Query(value = """
    WITH submitted_attempts AS (
        SELECT DISTINCT a.attempt_id
        FROM exam_attempt a
        JOIN exam_assignment ea
            ON ea.exam_id = a.exam_id
        JOIN class_enrollment_cache ce
            ON ce.class_offering_id = ea.class_offering_id
           AND ce.student_id = a.student_id
           AND UPPER(ce.status) = 'ENROLLED'
        WHERE a.exam_id = :examId
          AND a.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
          AND (:classOfferingId IS NULL OR ea.class_offering_id = :classOfferingId)
    ),
    answer_stats AS (
        SELECT
            q.question_id,
            COUNT(ans.answer_id) AS total_answered,
            SUM(CASE WHEN ans.is_correct = TRUE THEN 1 ELSE 0 END) AS correct_count,
            SUM(CASE WHEN ans.is_correct = FALSE THEN 1 ELSE 0 END) AS incorrect_count
        FROM exam_question q
        LEFT JOIN exam_answer ans
            ON ans.question_id = q.question_id
           AND ans.attempt_id IN (
                SELECT attempt_id
                FROM submitted_attempts
           )
        WHERE q.exam_id = :examId
        GROUP BY q.question_id
    )
    SELECT
        q.question_id,
        q.question_order,
        CAST(q.question_type AS TEXT) AS question_type,
        q.question_text,
        COALESCE(s.correct_count, 0) AS correct_count,
        COALESCE(s.incorrect_count, 0) AS incorrect_count,
        COALESCE(s.total_answered, 0) AS total_answered,
        COALESCE(
            COALESCE(s.correct_count, 0) * 100.0 / NULLIF(s.total_answered, 0),
            0
        ) AS correct_percentage,
        CASE
            WHEN COALESCE(
                COALESCE(s.correct_count, 0) * 100.0 / NULLIF(s.total_answered, 0),
                0
            ) >= 80 THEN 'Easy'
            WHEN COALESCE(
                COALESCE(s.correct_count, 0) * 100.0 / NULLIF(s.total_answered, 0),
                0
            ) >= 40 THEN 'Moderate'
            ELSE 'Difficult'
        END AS difficulty
    FROM exam_question q
    LEFT JOIN answer_stats s
        ON s.question_id = q.question_id
    WHERE q.exam_id = :examId
    ORDER BY q.question_order ASC
    """, nativeQuery = true)
    List<Object[]> findQuestionAnalysisRaw(
            @Param("examId") Long examId,
            @Param("classOfferingId") String classOfferingId
    );
}