package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamAttemptChoiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamAttemptChoiceOrderRepository extends JpaRepository<ExamAttemptChoiceOrder, Long> {

    List<ExamAttemptChoiceOrder> findByAttemptId(Long attemptId);
}
