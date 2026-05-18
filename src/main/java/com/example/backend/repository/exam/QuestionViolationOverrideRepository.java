package com.example.backend.repository.exam;

import com.example.backend.entity.exam.QuestionViolationOverride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionViolationOverrideRepository extends JpaRepository<QuestionViolationOverride, Long> {
    void deleteByQuestionExamExamId(Long examId);
}
