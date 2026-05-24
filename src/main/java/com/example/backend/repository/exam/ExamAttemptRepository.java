package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findByExamIdAndStudentId(Long examId, String studentId);

    List<ExamAttempt> findByExamId(Long examId);

    @Query("""
    SELECT COUNT(DISTINCT a.studentId)
    FROM ExamAttempt a
    WHERE a.examId = :examId
      AND a.status IN (
          com.example.backend.entity.enums.ExamAttemptStatus.SUBMITTED,
          com.example.backend.entity.enums.ExamAttemptStatus.AUTO_SUBMITTED
      )
""")
    int countFinishedAttemptsByExamId(@Param("examId") Long examId);

}
