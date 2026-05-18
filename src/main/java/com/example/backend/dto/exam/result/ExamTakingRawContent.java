package com.example.backend.dto.exam.result;

import com.example.backend.entity.exam.Exam;
import com.example.backend.entity.exam.ExamChoice;
import com.example.backend.entity.exam.ExamQuestion;

import java.util.List;
import java.util.Map;

public class ExamTakingRawContent {

    private Exam exam;
    private List<ExamQuestion> questions;
    private Map<Long, List<ExamChoice>> choiceMap;

    public ExamTakingRawContent(
            Exam exam,
            List<ExamQuestion> questions,
            Map<Long, List<ExamChoice>> choiceMap
    ) {
        this.exam = exam;
        this.questions = questions;
        this.choiceMap = choiceMap;
    }

    public Exam getExam() {
        return exam;
    }

    public List<ExamQuestion> getQuestions() {
        return questions;
    }

    public Map<Long, List<ExamChoice>> getChoiceMap() {
        return choiceMap;
    }
}
