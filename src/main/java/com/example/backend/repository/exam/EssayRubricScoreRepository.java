package com.example.backend.repository.exam;

import com.example.backend.entity.exam.EssayRubricScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EssayRubricScoreRepository
        extends JpaRepository<EssayRubricScore, Long> {

    Optional<EssayRubricScore> findByAnswerAnswerIdAndRubricRubricId(
            Long answerId,
            Long rubricId
    );

    List<EssayRubricScore> findByAnswerAnswerIdIn(List<Long> answerIds);
}
