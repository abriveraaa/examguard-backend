package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamAnswer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExamAnswerRepository
        extends JpaRepository<ExamAnswer, Long> {

    List<ExamAnswer> findByAttemptAttemptId(Long attemptId);

    boolean existsByQuestionQuestionId(Long questionId);

    @Query("""
    SELECT COUNT(a) > 0
    FROM ExamAnswer a
    WHERE a.attempt.attemptId = :attemptId
      AND (
          a.needsChecking = true
          OR UPPER(a.reviewStatus) IN ('PENDING', 'FLAGGED')
      )
""")
    boolean existsPendingReviewByAttemptId(@Param("attemptId") Long attemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExamAnswer> findByAttemptAttemptIdAndQuestionQuestionId(
            Long attemptId,
            Long questionId
    );

    @Query("""
    SELECT COALESCE(SUM(a.pointsAwarded), 0)
    FROM ExamAnswer a
    WHERE a.attempt.attemptId = :attemptId
""")
    BigDecimal sumPointsAwardedByAttemptId(@Param("attemptId") Long attemptId);

    boolean existsByAttemptAttemptIdAndQuestionQuestionId(
            Long attemptId,
            Long questionId
    );

    @Query("""
    SELECT DISTINCT a
    FROM ExamAnswer a
    JOIN FETCH a.question q
    LEFT JOIN FETCH a.selectedChoiceId sc
    LEFT JOIN FETCH a.rubricScores rs
    LEFT JOIN FETCH rs.rubric r
    WHERE a.attempt.attemptId = :attemptId
    ORDER BY q.questionOrder ASC
""")
    List<ExamAnswer> findResultAnswersByAttemptId(
            @Param("attemptId") Long attemptId
    );

}

