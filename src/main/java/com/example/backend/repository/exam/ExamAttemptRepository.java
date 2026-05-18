package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findByExamIdAndStudentId(Long examId, String studentId);

    List<ExamAttempt> findByExamId(Long examId);

}
