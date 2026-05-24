package com.example.backend.repository.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.entity.exam.ExamAnswerReviewLog;
import com.example.backend.dto.faculty.response.AnswerReviewTimelineDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamAnswerReviewLogRepository
        extends JpaRepository<ExamAnswerReviewLog, Long> {

    List<ExamAnswerReviewLog>
    findByAnswerAnswerIdOrderByCreatedAtAsc(Long answerId);

    List<ExamAnswerReviewLog>
    findByAttemptAttemptIdOrderByCreatedAtAsc(Long attemptId);

    Optional<ExamAnswerReviewLog>
    findTopByViolationViolationIdAndActionTypeOrderByCreatedAtDesc(
            Long violationId,
            String actionType
    );

    @Query("""
    SELECT log
    FROM ExamAnswerReviewLog log
    JOIN FETCH log.answer a
    LEFT JOIN FETCH log.violation v
    WHERE a.answerId IN :answerIds
      AND log.notes IS NOT NULL
      AND TRIM(log.notes) <> ''
    ORDER BY log.createdAt ASC
""")
    List<ExamAnswerReviewLog> findFeedbackLogsByAnswerIds(
            @Param("answerIds") List<Long> answerIds
    );

    @Query("""
    SELECT new com.example.backend.dto.faculty.response.AnswerReviewTimelineDTO(
        log.reviewLogId,
        log.actionType,
        log.previousValue,
        log.newValue,
        log.scoreBefore,
        log.scoreAfter,
        log.deduction,
        log.notes,
        log.createdBy,
        log.createdByRole,
        log.createdAt
    )
    FROM ExamAnswerReviewLog log
    WHERE log.answer.answerId = :answerId
    ORDER BY log.createdAt ASC
""")
    List<AnswerReviewTimelineDTO> findTimelineByAnswerId(
            @Param("answerId") Long answerId
    );
}