package com.example.backend.dto.exam.response;

import com.example.backend.entity.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ExamResponse {

    private Long examId;
    private String title;
    private String description;
    private String dateCreated;
    private String validUntil;
    private String status;
    private String duration;
    private String assigned;
    private String takers;
    private String startDateTime;
    private String endDateTime;
    private String examMode;
    private String createdBy;
    private String updatedBy;
    private Integer timeLimitMinutes;
    private Boolean shuffleQuestions;
    private Boolean shuffleChoices;
    private OffsetDateTime rawStartDateTime;
    private OffsetDateTime rawEndDateTime;
    private String term;
    private String academicYear;
    private List<String> classOfferingIds = new ArrayList<>();
    private List<QuestionPreview> questions = new ArrayList<>();

    public ExamResponse() {
    }

    // TableView response
    public ExamResponse(
            Long examId,
            String title,
            String description,
            String dateCreated,
            String validUntil,
            String status,
            String duration,
            String assigned,
            String term,
            String academicYear,
            String takers,
            String startDateTime,
            String endDateTime,
            String examMode,
            String createdBy,
            String updatedBy
    ) {
        this.examId = examId;
        this.title = title;
        this.description = description;
        this.dateCreated = dateCreated;
        this.validUntil = validUntil;
        this.status = status;
        this.duration = duration;
        this.assigned = assigned;
        this.term = term;
        this.academicYear = academicYear;
        this.takers = takers;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.examMode = examMode;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    // Detail / preview / edit response
    public ExamResponse(
            Long examId,
            String title,
            String description,
            String dateCreated,
            String validUntil,
            String status,
            String duration,
            String assigned,
            String term,
            String academicYear,
            String takers,
            String startDateTime,
            String endDateTime,
            String examMode,
            String createdBy,
            String updatedBy,
            List<QuestionPreview> questions
    ) {
        this(
                examId,
                title,
                description,
                dateCreated,
                validUntil,
                status,
                duration,
                assigned,
                term,
                academicYear,
                takers,
                startDateTime,
                endDateTime,
                examMode,
                createdBy,
                updatedBy
        );

        this.questions = questions == null ? new ArrayList<>() : questions;
    }

    public void setClassOfferingIds(List<String> classOfferingIds) {
        this.classOfferingIds = classOfferingIds == null ? new ArrayList<>() : classOfferingIds;
    }

    @Getter
    @Setter
    public static class QuestionPreview {


        private Long questionId;
        private QuestionType questionType;
        private String questionText;
        private String questionImageUrl;
        private BigDecimal points;
        private Integer questionOrder;
        private String correctAnswer;
        private List<ChoicePreview> choices = new ArrayList<>();
        private String questionInstruction;
        private List<EssayRubricResponse> rubrics;

        public QuestionPreview() {
        }

        public QuestionPreview(
                Long questionId,
                QuestionType questionType,
                String questionText,
                String questionImageUrl,
                BigDecimal points,
                Integer questionOrder,
                String correctAnswer,
                List<ChoicePreview> choices,
                String questionInstruction,
                List<EssayRubricResponse> rubrics
        ) {
            this.questionId = questionId;
            this.questionType = questionType;
            this.questionText = questionText;
            this.questionImageUrl = questionImageUrl;
            this.points = points;
            this.questionOrder = questionOrder;
            this.correctAnswer = correctAnswer;
            this.questionInstruction = questionInstruction;
            this.rubrics = rubrics;

            if (choices != null) {
                this.choices.addAll(choices);
            }
        }
    }


    @Getter
    @Setter
    public static class ChoicePreview {

        private Long choiceId;
        private String choiceLabel;
        private String choiceText;
        private String choiceImageUrl;
        private boolean correct;

        public ChoicePreview() {
        }

        public ChoicePreview(
                Long choiceId,
                String choiceLabel,
                String choiceText,
                String choiceImageUrl,
                boolean correct
        ) {
            this.choiceId = choiceId;
            this.choiceLabel = choiceLabel;
            this.choiceText = choiceText;
            this.choiceImageUrl = choiceImageUrl;
            this.correct = correct;
        }

    }

    @Getter
    @Setter
    public static class EssayRubricResponse {
        private Long rubricId;
        private String criterionName;
        private BigDecimal weightPercentage;
        private String description;
        private Integer displayOrder;

        public EssayRubricResponse(
                Long rubricId,
                String criterionName,
                BigDecimal weightPercentage,
                String description,
                Integer displayOrder
        ) {
            this.rubricId = rubricId;
            this.criterionName = criterionName;
            this.weightPercentage = weightPercentage;
            this.description = description;
            this.displayOrder = displayOrder;
        }

    }
}