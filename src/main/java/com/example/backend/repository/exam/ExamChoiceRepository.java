package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamChoiceRepository extends JpaRepository<ExamChoice, Long> {
    List<ExamChoice> findByQuestionQuestionIdInOrderByQuestionQuestionIdAscChoiceOrderAsc( List<Long> questionIds );
    List<ExamChoice> findByQuestionQuestionIdOrderByChoiceOrderAsc(Long questionId);
    void deleteByQuestionExamExamId(Long examId);

}