package com.example.backend.repository.exam;

import com.example.backend.entity.exam.EssayRubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EssayRubricRepository extends JpaRepository<EssayRubric, Long> {

    List<EssayRubric> findByQuestionQuestionIdInOrderByQuestionQuestionIdAscDisplayOrderAsc(List<Long> questionIds);
    List<EssayRubric> findByQuestionQuestionIdOrderByDisplayOrderAsc(Long questionId);
    void deleteByQuestionExamExamId(Long examId);
}