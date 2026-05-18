package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByExamExamIdOrderByQuestionOrderAsc(Long examId);
    void deleteByExamExamId(Long examId);

    @Query("""
    SELECT COALESCE(SUM(q.points), 0)
    FROM ExamQuestion q
    WHERE q.exam.examId = :examId
""")
    BigDecimal sumTotalPointsByExamId(@Param("examId") Long examId);

}