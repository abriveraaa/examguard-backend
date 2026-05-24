package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamViolationSetting;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamViolationSettingRepository extends JpaRepository<ExamViolationSetting, Long> {
    List<ExamViolationSetting> findByExamExamId(Long examId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ExamViolationSetting s WHERE s.exam.examId = :examId")
    void deleteByExamIdNow(@Param("examId") Long examId);
}