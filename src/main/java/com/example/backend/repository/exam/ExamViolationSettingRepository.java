package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamViolationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamViolationSettingRepository extends JpaRepository<ExamViolationSetting, Long> {
    List<ExamViolationSetting> findByExamExamId(Long examId);
    void deleteByExamExamId(Long examId);
}