package com.example.backend.service.student;

import com.example.backend.audit.ActivityTarget;
import com.example.backend.audit.ActivityTargetType;
import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.dto.student.StudentResultHeaderDTO;
import com.example.backend.dto.student.dashboard.*;
import com.example.backend.dto.student.result.*;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.StudentDashboardView;
import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.exam.*;
import com.example.backend.repository.core.StudentDashboardViewRepository;
import com.example.backend.repository.exam.*;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final ExamViolationLogRepository violationLogRepository;
    private final ExamRepository examRepository;
    private final StudentDashboardViewRepository dashboardViewRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamChoiceRepository examChoiceRepository;
    private final EssayRubricRepository essayRubricRepository;
    private final StudentReadCacheService studentReadCacheService;


    // =====================
    // STUDENT DASHBOARD
    // =====================

    @TrackActivity(
            module = "STUDENT_DASHBOARD",
            action = "VIEW_DASHBOARD",
            message = "Student dashboard viewed"
    )
    public StudentResponse getDashboard(String userId, String role) {
        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only students can access this dashboard.");
        }

        StudentProfileDTO profileDto =
                studentReadCacheService.getProfile(userId);

        List<StudentUpcomingExamDTO> upcomingExams =
                studentReadCacheService.getUpcomingExams(userId);

        List<StudentResultSummaryDTO> resultSummary =
                studentReadCacheService.getResults(userId);

        List<StudentViolationSummaryDTO> violations =
                studentReadCacheService.getViolations(userId);

        StudentDashboardStatsDTO stats = StudentDashboardStatsDTO.builder()
                .upcomingExamCount(upcomingExams.size())
                .completedExamCount(resultSummary.size())
                .violationCount(violations.size())
                .averageScore(0.0)
                .build();

        return StudentResponse.builder()
                .profile(profileDto)
                .upcomingExams(upcomingExams)
                .resultSummary(resultSummary)
                .violations(violations)
                .stats(stats)
                .build();
    }

    @TrackActivity(
            module = "STUDENT_DASHBOARD",
            action = "MARK_ITEM_VIEWED",
            message = "Student marked dashboard item as viewed"
    )
    @Transactional
    public void markDashboardItemViewed(
            String studentId,
            String itemType,
            Long itemId
    ) {
        if (!dashboardViewRepository.existsByStudentIdAndItemTypeAndItemId(
                studentId,
                itemType,
                itemId
        )) {
            dashboardViewRepository.save(
                    StudentDashboardView.builder()
                            .studentId(studentId)
                            .itemType(itemType)
                            .itemId(itemId)
                            .viewedAt(OffsetDateTime.now())
                            .build()
            );
        }
    }

    @TrackActivity(
            module = "STUDENT_RESULTS",
            action = "VIEW_EXAM_RESULT",
            message = "Student viewed exam result"
    )
    @Transactional(readOnly = true)
    public StudentExamResultResponse getStudentExamResult(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,
            String studentId
    ) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        if (!Boolean.TRUE.equals(exam.getResultsReleased())) {
            throw new RuntimeException("Results are not yet released for this exam.");
        }

        ExamAttempt attempt = examAttemptRepository
                .findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new RuntimeException("No submitted attempt found for this exam."));

        List<ExamAnswer> answers =
                examAnswerRepository.findResultAnswersByAttemptId(attempt.getAttemptId());

        List<Long> questionIds = answers.stream()
                .map(answer -> answer.getQuestion().getQuestionId())
                .distinct()
                .toList();

        Map<Long, List<ExamChoice>> choicesByQuestionId =
                examChoiceRepository
                        .findByQuestionQuestionIdInOrderByQuestionQuestionIdAscChoiceOrderAsc(questionIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                choice -> choice.getQuestion().getQuestionId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        Map<Long, List<ExamViolationLog>> violationsByQuestionId =
                violationLogRepository
                        .findByAttemptAttemptIdOrderByOccurredAtAsc(
                                attempt.getAttemptId()
                        )
                        .stream()
                        .filter(v -> v.getQuestion() != null)
                        .filter(v ->
                                !"IGNORED".equalsIgnoreCase(
                                        v.getReviewStatus()
                                )
                        )
                        .collect(Collectors.groupingBy(
                                v -> v.getQuestion().getQuestionId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<StudentExamResultQuestionResponse> questionResponses =
                answers.stream()
                        .map(answer -> buildResultQuestion(
                                answer,
                                choicesByQuestionId.getOrDefault(
                                        answer.getQuestion().getQuestionId(),
                                        List.of()
                                ),
                                violationsByQuestionId.getOrDefault(
                                        answer.getQuestion().getQuestionId(),
                                        List.of()
                                )
                        ))
                        .toList();

        double totalPoints = answers.stream()
                .map(ExamAnswer::getQuestion)
                .map(ExamQuestion::getPoints)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        StudentResultHeaderDTO header =
                examRepository.findStudentResultHeader(examId, studentId)
                        .orElse(new StudentResultHeaderDTO(null, null, null, null, null));

        return new StudentExamResultResponse(
                exam.getExamId(),
                attempt.getAttemptId(),
                exam.getTitle(),
                header.getCourseCode(),
                header.getCourseDescription(),
                header.getFaculty(),
                header.getTerm(),
                header.getAcademicYear(),
                exam.getTimeLimitMinutes(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getTotalScore(),
                totalPoints,
                attempt.getScorePercentage(),
                attempt.getStatus() == null ? null : attempt.getStatus().name(),
                attempt.getReviewStatus(),
                exam.getResultsReleased(),
                questionResponses
        );
    }


    // ==================
    // EXAM LIST
    // ==================

    @TrackActivity(
            module = "STUDENT_EXAMS",
            action = "VIEW_EXAM_CARDS",
            message = "Student viewed exam cards"
    )
    public List<StudentExamCardDTO> getStudentExamCards(String studentId) {
        OffsetDateTime now = OffsetDateTime.now();

        List<StudentExamCardDTO> exams =
                studentReadCacheService.getStudentExams(studentId);

        for (StudentExamCardDTO exam : exams) {
            String status = computeStatus(exam, now);
            exam.setStatus(status);
            exam.setActionable(isActionable(status));
        }

        return exams;
    }


    // ==================
    // HELPER
    // ==================

    private StudentExamResultQuestionResponse buildResultQuestion(
            ExamAnswer answer,
            List<ExamChoice> choices,
            List<ExamViolationLog> violations
    ) {
        ExamQuestion question = answer.getQuestion();

        List<StudentExamResultChoiceResponse> choiceResponses =
                choices.stream()
                        .map(choice -> new StudentExamResultChoiceResponse(
                                choice.getChoiceId(),
                                choice.getChoiceLabel(),
                                choice.getChoiceText(),
                                choice.getChoiceImageUrl(),
                                Boolean.TRUE.equals(choice.getCorrect()),
                                answer.getSelectedChoiceId() != null
                                        && Objects.equals(
                                        answer.getSelectedChoiceId().getChoiceId(),
                                        choice.getChoiceId()
                                )
                        ))
                        .toList();

        List<StudentExamResultViolationResponse> violationResponses =
                violations.stream()
                        .map(v -> new StudentExamResultViolationResponse(
                                v.getViolationId(),
                                v.getViolationType(),
                                v.getSeverity(),
                                v.getViolationMessage(),
                                v.getReviewStatus(),
                                v.getReviewNotes(),
                                v.getEvidenceUrl(),
                                v.getOccurredAt()
                        ))
                        .toList();

        List<StudentExamResultRubricResponse> rubricResponses =
                buildRubricResponses(answer);

        return new StudentExamResultQuestionResponse(
                question.getQuestionId(),
                question.getQuestionOrder(),
                question.getQuestionType() == null ? null : question.getQuestionType().name(),
                question.getQuestionText(),
                question.getQuestionImageUrl(),
                question.getPoints() == null ? 0.0 : question.getPoints().doubleValue(),
                answer.getPointsAwarded() == null ? 0.0 : answer.getPointsAwarded().doubleValue(),
                answer.getAnswerText(),
                resolveCorrectAnswer(question, choices),
                answer.getIsCorrect(),
                answer.getFacultyFeedback(),
                choiceResponses,
                violationResponses,
                rubricResponses
        );
    }

    private List<StudentExamResultRubricResponse> buildRubricResponses(
            ExamAnswer answer
    ) {
        if (answer.getQuestion() == null
                || answer.getQuestion().getQuestionType() == null
                || !"ESSAY".equalsIgnoreCase(answer.getQuestion().getQuestionType().name())) {
            return List.of();
        }

        Map<Long, EssayRubricScore> scoreByRubricId =
                answer.getRubricScores() == null
                        ? Map.of()
                        : answer.getRubricScores()
                          .stream()
                          .filter(score -> score.getRubric() != null)
                          .collect(Collectors.toMap(
                                  score -> score.getRubric().getRubricId(),
                                  score -> score,
                                  (a, b) -> a
                          ));

        List<EssayRubric> rubrics =
                essayRubricRepository.findByQuestionQuestionIdOrderByDisplayOrderAsc(
                        answer.getQuestion().getQuestionId()
                );

        return rubrics.stream()
                .map(rubric -> {
                    EssayRubricScore score =
                            scoreByRubricId.get(rubric.getRubricId());

                    return new StudentExamResultRubricResponse(
                            rubric.getRubricId(),
                            rubric.getCriterionName(),
                            rubric.getWeightPercentage() == null
                                    ? 0.0
                                    : rubric.getWeightPercentage().doubleValue(),
                            rubric.getDescription(),
                            rubric.getDisplayOrder(),
                            score == null || score.getScoreAwarded() == null
                                    ? 0.0
                                    : score.getScoreAwarded().doubleValue(),
                            score == null || score.getScorePercentage() == null
                                    ? 0.0
                                    : score.getScorePercentage().doubleValue(),
                            score == null ? null : score.getFeedback()
                    );
                })
                .toList();
    }

    private String resolveCorrectAnswer(
            ExamQuestion question,
            List<ExamChoice> choices
    ) {

        if (question.getQuestionType() == null) {
            return null;
        }

        String type = question.getQuestionType().name();

        if ("MULTIPLE_CHOICE".equalsIgnoreCase(type)) {

            return choices.stream()
                    .filter(choice ->
                            Boolean.TRUE.equals(choice.getCorrect()))
                    .map(ExamChoice::getChoiceText)
                    .findFirst()
                    .orElse(null);
        }

        return question.getCorrectAnswer();
    }

    private String computeStatus(StudentExamCardDTO exam, OffsetDateTime now) {

        String attemptStatus = exam.getAttemptStatus();

        if (Boolean.TRUE.equals(exam.getResultsReleased())) {
            return "RESULTS RELEASED";
        }

        if ("SUBMITTED".equalsIgnoreCase(attemptStatus)
                || "AUTO_SUBMITTED".equalsIgnoreCase(attemptStatus)) {
            return "PENDING REVIEW";
        }

        OffsetDateTime start = exam.getStartDateTime();
        OffsetDateTime end = exam.getEndDateTime();

        if (start == null || end == null) {
            return "UPCOMING";
        }

        int durationMinutes =
                exam.getDurationMinutes() == null
                        ? 60
                        : exam.getDurationMinutes();

        OffsetDateTime effectiveEnd =
                exam.getMode() == ExamMode.SYNCHRONOUS
                        ? start.plusMinutes(durationMinutes)
                        : end;

        if (now.isBefore(start)) {
            return "UPCOMING";
        }

        if ("IN_PROGRESS".equalsIgnoreCase(attemptStatus)) {
            return now.isBefore(effectiveEnd)
                    ? "ONGOING"
                    : "DID NOT TAKE";
        }

        if (now.isBefore(effectiveEnd)) {
            return "ONGOING";
        }

        return "DID NOT TAKE";
    }

    private Boolean isActionable(String status) {
        return switch (status) {
            case "ONGOING", "RESULTS RELEASED" -> true;
            default -> false;
        };
    }

}
