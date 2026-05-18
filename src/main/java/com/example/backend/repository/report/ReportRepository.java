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
}