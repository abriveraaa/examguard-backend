package com.example.backend.dto.exam.response;

import com.example.backend.dto.exam.request.EssayRubricRequest;
import com.example.backend.entity.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ExamTakingQuestionResponse implements Serializable {
    private Long questionId;
    private QuestionType questionType;
    private String questionText;
    private String questionImageUrl;
    private BigDecimal points;
    private List<ExamTakingChoiceResponse> choices;
    private String questionInstruction;
    private List<EssayRubricRequest> rubrics;
    private Long selectedChoiceId;
    private String studentAnswer;

    public ExamTakingQuestionResponse(
            Long questionId,
            QuestionType questionType,
            String questionText,
            String questionImageUrl,
            BigDecimal points,
            List<ExamTakingChoiceResponse> choices,
            String questionInstruction,
            List<EssayRubricRequest> rubrics
    ) {
        this.questionId = questionId;
        this.questionType = questionType;
        this.questionText = questionText;
        this.questionImageUrl = questionImageUrl;
        this.points = points;
        this.choices = choices;
        this.questionInstruction = questionInstruction;
        this.rubrics = rubrics;
    }

}
