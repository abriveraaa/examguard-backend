package com.example.backend.repository.exam;

import com.example.backend.dto.faculty.*;
import com.example.backend.entity.exam.Exam;
import com.example.backend.dto.faculty.response.FacultyExamDetailResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamWorkspaceRepository extends JpaRepository<Exam, Long> {

    @Query("""
    SELECT new com.example.backend.dto.faculty.FacultyExamSummaryDTO(
        e.examId,
        e.title,

        MIN(co.courseCode),
        MIN(co.courseDescription),

        MIN(co.programCode),
        '',
        CAST(e.status AS string),

        e.startDateTime,
        e.endDateTime,

        COUNT(DISTINCT enrollment.studentId),

        COUNT(DISTINCT attempt.attemptId),

        COUNT(DISTINCT v.attempt.studentId)
    )

    FROM Exam e

    JOIN ExamAssignment ass
        ON ass.exam.examId = e.examId

    JOIN FacultyLoadCache fl
        ON fl.classOfferingId = ass.classOfferingId

    JOIN ClassOfferingCache co
        ON co.classOfferingId = ass.classOfferingId

    LEFT JOIN ClassEnrollmentCache enrollment
        ON enrollment.classOfferingId = ass.classOfferingId
       AND enrollment.status = 'ENROLLED'

    LEFT JOIN ExamAttempt attempt
        ON attempt.examId = e.examId
       AND attempt.studentId = enrollment.studentId
       AND attempt.status IN (
            com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
            com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
       )

    LEFT JOIN ExamViolationLog v
        ON v.attempt.attemptId = attempt.attemptId

    WHERE fl.employeeId = :employeeId

    GROUP BY
        e.examId,
        e.title,
        e.status,
        e.startDateTime,
        e.endDateTime

    ORDER BY e.startDateTime DESC
""")
    List<FacultyExamSummaryDTO> findFacultyExamSummaries(
            @Param("employeeId") String employeeId
    );

    @Query("""
    SELECT
        e.examId,
        CONCAT(
            co.programCode,
            ' ',
            co.yearLevel,
            '-',
            co.sectionName
        )
    FROM Exam e
    JOIN ExamAssignment ass
        ON ass.exam.examId = e.examId
    JOIN FacultyLoadCache fl
        ON fl.classOfferingId = ass.classOfferingId
    JOIN ClassOfferingCache co
        ON co.classOfferingId = ass.classOfferingId
    WHERE fl.employeeId = :employeeId
""")
    List<Object[]> findExamSectionMappings(
            @Param("employeeId") String employeeId
    );

    @Query("""
SELECT new com.example.backend.dto.faculty.response.FacultyExamDetailResponse(
     e.examId,
     e.title,
     e.description,
     CAST(e.status AS string),
     e.timeLimitMinutes,
     e.startDateTime,
     e.endDateTime,
     e.shuffleQuestions,
     e.shuffleChoices,
     COUNT(DISTINCT enrollment.studentId),
     COUNT(DISTINCT attempt.attemptId),
     CAST(0 AS long),
     COUNT(DISTINCT v.attempt.studentId),
     COUNT(DISTINCT v.violationId),
     e.resultsReleased
 )
FROM Exam e
JOIN ExamAssignment ass
    ON ass.exam.examId = e.examId
LEFT JOIN FacultyLoadCache fl
    ON fl.classOfferingId = ass.classOfferingId
LEFT JOIN ClassEnrollmentCache enrollment
    ON enrollment.classOfferingId = ass.classOfferingId
   AND enrollment.status = 'ENROLLED'
LEFT JOIN ExamAttempt attempt
    ON attempt.examId = e.examId
   AND attempt.studentId = enrollment.studentId
   AND attempt.status IN (
        com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
        com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
   )
LEFT JOIN ExamViolationLog v
    ON v.attempt.attemptId = attempt.attemptId
WHERE e.examId = :examId
  AND (
        UPPER(:role) = 'ADMIN'
        OR fl.employeeId = :employeeId
  )
GROUP BY
    e.examId,
    e.title,
    e.description,
    e.status,
    e.timeLimitMinutes,
    e.startDateTime,
    e.endDateTime,
    e.shuffleQuestions,
    e.shuffleChoices,
    e.resultsReleased
""")
    FacultyExamDetailResponse findExamDetail(
            @Param("examId") Long examId,
            @Param("employeeId") String employeeId,
            @Param("role") String role
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.FacultyClassDTO(
            co.classOfferingId,
            co.courseCode,
            co.courseDescription,
            co.programCode,
            co.sectionName,
            co.yearLevel,
            co.academicYear,
            co.term,
            COUNT(enrollment.studentId)
        )
        FROM ExamAssignment ass
        LEFT JOIN FacultyLoadCache fl
            ON fl.classOfferingId = ass.classOfferingId
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ass.classOfferingId
        LEFT JOIN ClassEnrollmentCache enrollment
            ON enrollment.classOfferingId = ass.classOfferingId
           AND enrollment.status = 'ENROLLED'
        WHERE ass.exam.examId = :examId
          AND (
                UPPER(:role) = 'ADMIN'
                OR fl.employeeId = :employeeId
          )
        GROUP BY
            co.classOfferingId,
            co.courseCode,
            co.courseDescription,
            co.programCode,
            co.sectionName,
            co.yearLevel,
            co.academicYear,
            co.term
        ORDER BY co.courseCode, co.sectionName
""")
    List<FacultyClassDTO> findExamAssignedClasses(
            @Param("examId") Long examId,
            @Param("employeeId") String employeeId,
            @Param("role") String role
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.FacultySubmissionSummaryDTO(
            attempt.attemptId,
            e.examId,
            e.title,
            attempt.studentId,
            CONCAT(s.firstName, ' ', s.lastName),
            co.courseCode,
            co.sectionName,
            CAST(attempt.status AS string),
            attempt.startedAt,
            attempt.submittedAt,
            attempt.scorePercentage,
            COUNT(DISTINCT v.violationId),
            CASE
                     WHEN attempt.scorePercentage IS NULL THEN true
                     WHEN COUNT(DISTINCT essayAnswer.answerId) > 0 THEN true
                     WHEN COUNT(DISTINCT v.violationId) > 0 THEN true
                     ELSE false
                 END
        )
        FROM ExamAttempt attempt
        JOIN Exam e
            ON e.examId = attempt.examId
        JOIN ExamAssignment ass
            ON ass.exam.examId = e.examId
        LEFT JOIN FacultyLoadCache fl
            ON fl.classOfferingId = ass.classOfferingId
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ass.classOfferingId
        JOIN ClassEnrollmentCache enrollment
            ON enrollment.classOfferingId = ass.classOfferingId
           AND enrollment.studentId = attempt.studentId
           AND enrollment.status = 'ENROLLED'
        LEFT JOIN StudentProfileCache s
            ON s.studentId = attempt.studentId
        LEFT JOIN ExamViolationLog v
            ON v.attempt.attemptId = attempt.attemptId
        LEFT JOIN ExamAnswer essayAnswer
            ON essayAnswer.attempt.attemptId = attempt.attemptId
           AND essayAnswer.question.questionType =
                com.example.backend.entity.enums.QuestionType.ESSAY
        WHERE e.examId = :examId
          AND ( UPPER(:role) = 'ADMIN' OR fl.employeeId = :employeeId )
          AND attempt.status IN (
                com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
                com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
          )
        GROUP BY
            attempt.attemptId,
            e.examId,
            e.title,
            attempt.studentId,
            s.firstName,
            s.lastName,
            co.courseCode,
            co.sectionName,
            attempt.status,
            attempt.startedAt,
            attempt.submittedAt,
            attempt.scorePercentage
        ORDER BY attempt.submittedAt DESC
    """)
    List<FacultySubmissionSummaryDTO> findExamSubmissions(
            @Param("examId") Long examId,
            @Param("employeeId") String employeeId,
            @Param("role") String role
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.FacultyStudentViolationDTO(
            attempt.attemptId,
            e.examId,
            attempt.studentId,
            CONCAT(s.firstName, ' ', s.lastName),
            co.courseCode,
            co.sectionName,
            COUNT(DISTINCT v.violationId),
            CASE
                WHEN COUNT(DISTINCT v.violationId) = 1 THEN MAX(v.violationType)
                ELSE 'MULTIPLE VIOLATIONS'
            END,
            MAX(v.severity),
            MAX(v.occurredAt),
            'FOR_REVIEW'
        )
        FROM ExamViolationLog v
        JOIN ExamAttempt attempt
            ON attempt.attemptId = v.attempt.attemptId
        JOIN Exam e
            ON e.examId = attempt.examId
        JOIN ExamAssignment ass
            ON ass.exam.examId = e.examId
        LEFT JOIN FacultyLoadCache fl
            ON fl.classOfferingId = ass.classOfferingId
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ass.classOfferingId
        JOIN ClassEnrollmentCache enrollment
            ON enrollment.classOfferingId = ass.classOfferingId
           AND enrollment.studentId = attempt.studentId
           AND enrollment.status = 'ENROLLED'
        LEFT JOIN StudentProfileCache s
            ON s.studentId = attempt.studentId
        WHERE e.examId = :examId
          AND ( UPPER(:role) = 'ADMIN' OR fl.employeeId = :employeeId )
        GROUP BY
            attempt.attemptId,
            e.examId,
            attempt.studentId,
            s.firstName,
            s.lastName,
            co.courseCode,
            co.sectionName
        ORDER BY MAX(v.occurredAt) DESC
    """)
    List<FacultyStudentViolationDTO> findExamViolations(
            @Param("examId") Long examId,
            @Param("employeeId") String employeeId,
            @Param("role") String role
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.FacultyExamStudentDTO(
            s.studentId,
            CONCAT(s.firstName, ' ', s.lastName),
            s.emailAddress,
            CONCAT(s.programCode, ' ', s.yearLevel, '-' , s.sectionName),
            COALESCE(CAST(attempt.status AS string), 'NOT_STARTED'),
            attempt.startedAt,
            attempt.submittedAt,
            attempt.scorePercentage,
            COUNT(DISTINCT v.violationId),
            CASE
                WHEN attempt.scorePercentage IS NULL
                     AND attempt.status IN (
                         com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
                         com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
                     )
                    THEN true
                WHEN COUNT(DISTINCT essayAnswer.answerId) > 0
                    THEN true
                WHEN COUNT(DISTINCT v.violationId) > 0
                    THEN true
                ELSE false
            END,
            attempt.reviewStatus
        )
        FROM ExamAssignment ass
        LEFT JOIN FacultyLoadCache fl
            ON fl.classOfferingId = ass.classOfferingId
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ass.classOfferingId
        JOIN ClassEnrollmentCache enrollment
            ON enrollment.classOfferingId = ass.classOfferingId
           AND enrollment.status = 'ENROLLED'
        JOIN StudentProfileCache s
            ON s.studentId = enrollment.studentId
        LEFT JOIN ExamAttempt attempt
            ON attempt.examId = ass.exam.examId
           AND attempt.studentId = enrollment.studentId
        LEFT JOIN ExamViolationLog v
            ON v.attempt.attemptId = attempt.attemptId
        LEFT JOIN ExamAnswer essayAnswer
            ON essayAnswer.attempt.attemptId = attempt.attemptId
           AND essayAnswer.question.questionType = com.example.backend.entity.enums.QuestionType.ESSAY
        WHERE ass.exam.examId = :examId
          AND (
                UPPER(:role) = 'ADMIN'
                OR fl.employeeId = :employeeId
          )
        GROUP BY
            s.studentId,
            s.firstName,
            s.lastName,
            s.emailAddress,
            CONCAT(s.programCode, ' ', s.yearLevel, '-', s.sectionName),
            attempt.status,
            attempt.startedAt,
            attempt.submittedAt,
            attempt.scorePercentage,
            attempt.reviewStatus
        ORDER BY s.lastName, s.firstName
""")
    List<FacultyExamStudentDTO> findExamStudents(
            @Param("examId") Long examId,
            @Param("employeeId") String employeeId,
            @Param("role") String role
    );

    @Query("""
    SELECT new com.example.backend.dto.faculty.FacultyLeaderboardDTO(
        attempt.attemptId,
        attempt.studentId,
        TRIM(CONCAT(
            COALESCE(student.firstName, ''),
            ' ',
            COALESCE(student.lastName, '')
        )),
        student.sectionName,
        attempt.totalScore,
        attempt.scorePercentage,
        COUNT(DISTINCT violation.violationId),
        attempt.startedAt,
        attempt.submittedAt,
        attempt.reviewStatus
    )
    FROM ExamAttempt attempt

    JOIN ExamAssignment ass
        ON ass.exam.examId = attempt.examId

    LEFT JOIN FacultyLoadCache load
        ON load.classOfferingId = ass.classOfferingId

    LEFT JOIN StudentProfileCache student
        ON student.studentId = attempt.studentId

    LEFT JOIN ExamViolationLog violation
        ON violation.attempt.attemptId = attempt.attemptId

    WHERE attempt.examId = :examId
      AND ( UPPER(:role) = 'ADMIN' OR load.employeeId = :employeeId )
      AND attempt.status IN (
            com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
            com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
      )

    GROUP BY
        attempt.attemptId,
        attempt.studentId,
        student.firstName,
        student.lastName,
        student.sectionName,
        attempt.totalScore,
        attempt.scorePercentage,
        attempt.startedAt,
        attempt.submittedAt,
        attempt.reviewStatus

    ORDER BY
        attempt.totalScore DESC,
        attempt.scorePercentage DESC,
        attempt.submittedAt ASC,
        attempt.studentId ASC
""")
    List<FacultyLeaderboardDTO> findExamLeaderboard(
            @Param("examId") Long examId,
            @Param("employeeId") String employeeId,
            @Param("role") String role
    );


    @Query("""
    SELECT new com.example.backend.dto.faculty.FacultyAttemptAnswerReviewDTO(
        answer.answerId,
        question.questionId,
        question.questionOrder,
        CAST(question.questionType AS string),
        question.questionText,
        CASE
            WHEN selectedChoice.choiceId IS NOT NULL THEN selectedChoice.choiceText
            ELSE COALESCE(answer.answerText, '')
        END,
        CASE
            WHEN correctChoice.choiceId IS NOT NULL THEN correctChoice.choiceText
            ELSE COALESCE(question.correctAnswer, '')
        END,
        question.points,
        answer.pointsAwarded,
        answer.isCorrect,
        CASE
            WHEN answer.manuallyReviewed = true THEN false
            WHEN question.questionType = com.example.backend.entity.enums.QuestionType.ESSAY THEN true
            WHEN COUNT(DISTINCT violation.violationId) > 0 THEN true
            ELSE false
        END,
        question.questionInstruction,
        answer.facultyFeedback,
        answer.reviewStatus,
        answer.needsChecking
    )
    FROM ExamQuestion question
    JOIN question.exam exam

    JOIN ExamAttempt attempt
        ON attempt.examId = exam.examId
       AND attempt.studentId = :studentId

    LEFT JOIN ExamAnswer answer
        ON answer.question.questionId = question.questionId
       AND answer.attempt.attemptId = attempt.attemptId

    LEFT JOIN answer.selectedChoiceId selectedChoice

    LEFT JOIN ExamChoice correctChoice
        ON correctChoice.question.questionId = question.questionId
       AND correctChoice.correct = true

    LEFT JOIN ExamViolationLog violation
        ON violation.attempt.attemptId = attempt.attemptId
       AND violation.question.questionId = question.questionId

    WHERE exam.examId = :examId
      AND attempt.status IN (
            com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
            com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
      )

    GROUP BY
        answer.answerId,
        question.questionId,
        question.questionOrder,
        question.questionType,
        question.questionText,
        selectedChoice.choiceId,
        selectedChoice.choiceText,
        correctChoice.choiceId,
        correctChoice.choiceText,
        answer.answerText,
        question.correctAnswer,
        question.points,
        answer.pointsAwarded,
        answer.isCorrect,
        answer.manuallyReviewed,
        question.questionInstruction,
        answer.facultyFeedback,
        answer.reviewStatus,
        answer.needsChecking

    ORDER BY question.questionOrder ASC
""")
    List<FacultyAttemptAnswerReviewDTO> findAttemptAnswersForReview(
            @Param("examId") Long examId,
            @Param("studentId") String studentId
    );

    @Query("""
    SELECT new com.example.backend.dto.faculty.FacultyAttemptViolationDTO(
         violation.violationId,
         question.questionId,
         violation.violationType,
         violation.severity,
         violation.violationMessage,
         violation.evidenceUrl,
         violation.attemptNumber,
         violation.occurredAt,
         violation.reviewStatus,
         violation.reviewedBy,
         violation.reviewedAt
     )
    FROM ExamViolationLog violation
    LEFT JOIN violation.question question
    JOIN violation.attempt attempt
    WHERE violation.exam.examId = :examId
      AND attempt.studentId = :studentId
    ORDER BY violation.occurredAt ASC
""")
    List<FacultyAttemptViolationDTO> findAttemptViolationsForReview(
            @Param("examId") Long examId,
            @Param("studentId") String studentId
    );

    @Query("""
    SELECT new com.example.backend.dto.faculty.FacultyActivityLogDTO(
        'ACTIVITY',
        log.logId,
        log.examId,
        log.attemptId,
        log.questionId,
        question.questionOrder,
        COALESCE(log.targetUserId, attempt.studentId),
        CONCAT(
            COALESCE(student.firstName, ''),
            ' ',
            COALESCE(student.lastName, '')
        ),
        log.module,
        log.action,
        null,
        log.message,
        null,
        log.durationMs,
        log.occurredAt
    )
    FROM SystemActivityLog log
    LEFT JOIN ExamAttempt attempt
        ON attempt.attemptId = log.attemptId
    LEFT JOIN ExamQuestion question
        ON question.questionId = log.questionId
    LEFT JOIN StudentProfileCache student
        ON student.studentId = COALESCE(log.targetUserId, attempt.studentId)
    WHERE log.examId = :examId
    ORDER BY log.occurredAt ASC
""")
    List<FacultyActivityLogDTO> findSystemActivityLogsByExamId(
            @Param("examId") Long examId
    );

    @Query("""
    SELECT new com.example.backend.dto.faculty.FacultyActivityLogDTO(
        'VIOLATION',
        violation.violationId,
        violation.exam.examId,
        attempt.attemptId,
        question.questionId,
        question.questionOrder,

        attempt.studentId,
        CONCAT(
            COALESCE(student.firstName, ''),
            ' ',
            COALESCE(student.lastName, '')
        ),

        'PROCTORING',
        violation.violationType,
        violation.severity,
        violation.violationMessage,
        violation.evidenceUrl,
        null,
        violation.occurredAt
    )
    FROM ExamViolationLog violation
    JOIN violation.attempt attempt
    LEFT JOIN violation.question question
    LEFT JOIN StudentProfileCache student
        ON student.studentId = attempt.studentId
    WHERE violation.exam.examId = :examId
    ORDER BY violation.occurredAt ASC
""")
    List<FacultyActivityLogDTO> findViolationActivityLogsByExamId(
            @Param("examId") Long examId
    );

    @Query("""
        SELECT CASE
            WHEN COUNT(DISTINCT e.examId) > 0 THEN true
            ELSE false
        END
        FROM Exam e
        LEFT JOIN ExamAssignment ass
            ON ass.exam.examId = e.examId
        LEFT JOIN FacultyLoadCache fl
            ON fl.classOfferingId = ass.classOfferingId
        WHERE e.examId = :examId
          AND (
                UPPER(:role) = 'ADMIN'
                OR fl.employeeId = :employeeId
          )
        """)
    boolean canAccessExam(
            @Param("examId") Long examId,
            @Param("employeeId") String employeeId,
            @Param("role") String role
    );

}
