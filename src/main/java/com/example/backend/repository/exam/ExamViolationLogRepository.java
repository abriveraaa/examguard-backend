package com.example.backend.repository.exam;

import com.example.backend.dto.student.dashboard.StudentViolationSummaryDTO;
import com.example.backend.entity.exam.ExamViolationLog;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.List;

public interface ExamViolationLogRepository extends JpaRepository<ExamViolationLog, Long> {

    List<ExamViolationLog> findByAttemptAttemptIdOrderByOccurredAtAsc(Long attemptId);

    List<ExamViolationLog> findByAttemptAttemptIdAndQuestionQuestionId(Long attemptId, Long questionId);

    boolean existsByAttemptAttemptIdAndQuestionQuestionId(
            Long attemptId,
            Long questionId
    );

    @Modifying
    @Transactional
    @Query("""
    UPDATE ExamViolationLog v
    SET v.reviewStatus = :reviewStatus,
        v.reviewedBy = :reviewedBy,
        v.reviewedAt = :reviewedAt
    WHERE v.attempt.attemptId = :attemptId
      AND v.question.questionId = :questionId
""")
    int markQuestionViolationsReviewed(
            @Param("attemptId") Long attemptId,
            @Param("questionId") Long questionId,
            @Param("reviewStatus") String reviewStatus,
            @Param("reviewedBy") String reviewedBy,
            @Param("reviewedAt") OffsetDateTime reviewedAt
    );

    @Query("""
    SELECT new com.example.backend.dto.student.dashboard.StudentViolationSummaryDTO(
        MIN(v.violationId),
        e.examId,
        MIN(co.courseCode),
        e.title,
        CASE
            WHEN MAX(
                CASE
                    WHEN UPPER(v.reviewStatus) IN ('REVIEWED', 'PENALIZED', 'IGNORED')
                    THEN 1
                    ELSE 0
                END
            ) = 1
            THEN 'Decision Released'
            ELSE 'Pending Decision'
        END
    )
    FROM ExamViolationLog v
    JOIN v.exam e
    JOIN v.attempt a
    JOIN ExamAssignment ass ON ass.exam.examId = e.examId
    JOIN ClassEnrollmentCache ce
        ON ce.studentId = a.studentId
       AND ce.classOfferingId = ass.classOfferingId
    JOIN ClassOfferingCache co ON co.classOfferingId = ass.classOfferingId
    WHERE a.studentId = :studentId
      AND UPPER(v.reviewStatus) IN ('PENDING_REVIEW', 'REVIEWED', 'PENALIZED', 'IGNORED')
      AND NOT EXISTS (
          SELECT 1
          FROM StudentDashboardView dv
          WHERE dv.studentId = :studentId
            AND dv.itemType = 'VIOLATION'
            AND dv.itemId = e.examId
      )
    GROUP BY e.examId, e.title
    ORDER BY MAX(v.occurredAt) DESC
""")
    List<StudentViolationSummaryDTO> findReviewedUnviewedViolationsForStudent(
            @Param("studentId") String studentId,
            Pageable pageable
    );

}
