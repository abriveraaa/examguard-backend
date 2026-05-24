package com.example.backend.repository;

import com.example.backend.dto.faculty.*;
import com.example.backend.dto.faculty.response.FacultyExamDetailResponse;
import com.example.backend.entity.exam.Exam;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacultyRepository extends Repository<Exam, Long> {

    @Query("""
        SELECT new com.example.backend.dto.faculty.FacultyProfileDTO(
            f.employeeId,
            f.firstName,
            f.lastName,
            f.emailAddress
        )
        FROM FacultyProfileCache f
        WHERE f.employeeId = :employeeId
    """)
    FacultyProfileDTO findFacultyProfile(@Param("employeeId") String employeeId);

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
        COUNT(DISTINCT enrollment.studentId)
    )
    FROM ClassOfferingCache co
    LEFT JOIN ClassEnrollmentCache enrollment
        ON enrollment.classOfferingId = co.classOfferingId
       AND enrollment.status = 'ENROLLED'
    WHERE co.classOfferingId IN (
        SELECT DISTINCT fl.classOfferingId
        FROM FacultyLoadCache fl
        WHERE fl.employeeId = :employeeId
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
    List<FacultyClassDTO> findAssignedClasses(@Param("employeeId") String employeeId);

    @Query("""
    SELECT DISTINCT new com.example.backend.dto.faculty.FacultySubmissionSummaryDTO(
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
    JOIN FacultyLoadCache fl
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
    WHERE fl.employeeId = :employeeId
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
    List<FacultySubmissionSummaryDTO> findRecentSubmissions(
            @Param("employeeId") String employeeId
    );

    @Query("""
        SELECT new com.example.backend.dto.faculty.FacultyViolationReviewDTO(
            e.examId,
            e.title,
            co.courseCode,
            COUNT(DISTINCT attempt.studentId),
            COUNT(DISTINCT v.violationId),
            MAX(v.occurredAt)
        )
        FROM ExamViolationLog v
        JOIN ExamAttempt attempt
            ON attempt.attemptId = v.attempt.attemptId
        JOIN Exam e
            ON e.examId = attempt.examId
        JOIN ExamAssignment ass
            ON ass.exam.examId = e.examId
        JOIN FacultyLoadCache fl
            ON fl.classOfferingId = ass.classOfferingId
        JOIN ClassOfferingCache co
            ON co.classOfferingId = ass.classOfferingId
        JOIN ClassEnrollmentCache enrollment
            ON enrollment.classOfferingId = ass.classOfferingId
           AND enrollment.studentId = attempt.studentId
           AND enrollment.status = 'ENROLLED'
       WHERE fl.employeeId = :employeeId
         AND attempt.status IN (
               com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
               com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
         )
         AND (
               attempt.reviewStatus IS NULL
               OR attempt.reviewStatus <> 'REVIEWED'
         )
        GROUP BY
            e.examId,
            e.title,
            co.courseCode
        ORDER BY MAX(v.occurredAt) DESC
    """)
    List<FacultyViolationReviewDTO> findViolationsForReview(
            @Param("employeeId") String employeeId
    );

}