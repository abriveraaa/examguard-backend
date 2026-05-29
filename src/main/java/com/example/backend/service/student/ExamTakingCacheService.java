package com.example.backend.service.student;

import com.example.backend.dto.exam.result.ExamTakingRawContent;
import com.example.backend.entity.exam.Exam;
import com.example.backend.entity.exam.ExamChoice;
import com.example.backend.entity.exam.ExamQuestion;
import com.example.backend.repository.exam.ExamChoiceRepository;
import com.example.backend.repository.exam.ExamQuestionRepository;
import com.example.backend.repository.exam.ExamRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExamTakingCacheService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamChoiceRepository choiceRepository;

    @Cacheable(value = "examTakingRawContent", key = "#examId")
    public ExamTakingRawContent getRawContent(Long examId) {
        return loadRawContentFromDatabase(examId);
    }

    @CachePut(value = "examTakingRawContent", key = "#examId")
    public ExamTakingRawContent warmCache(Long examId) {
        return loadRawContentFromDatabase(examId);
    }

    @CacheEvict(value = "examTakingRawContent", key = "#examId")
    public void evictCache(Long examId) { }

    private ExamTakingRawContent loadRawContentFromDatabase(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        List<ExamQuestion> questions =
                questionRepository.findByExamExamIdOrderByQuestionOrderAsc(examId);

        List<Long> questionIds = questions.stream()
                .map(ExamQuestion::getQuestionId)
                .toList();

        List<ExamChoice> allChoices = questionIds.isEmpty()
                ? new ArrayList<>()
                : choiceRepository.findByQuestionQuestionIdInOrderByQuestionQuestionIdAscChoiceOrderAsc(questionIds);

        Map<Long, List<ExamChoice>> choiceMap = allChoices.stream()
                .collect(Collectors.groupingBy(
                        choice -> choice.getQuestion().getQuestionId()
                ));

        return new ExamTakingRawContent(exam, questions, choiceMap);
    }
}