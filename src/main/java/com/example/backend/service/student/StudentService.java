package com.example.backend.service.student;

import com.example.backend.dto.core.CurrentTermDTO;
import com.example.backend.dto.student.StudentResultHeaderDTO;
import com.example.backend.dto.student.dashboard.*;
import com.example.backend.dto.student.result.*;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.StudentDashboardView;
import com.example.backend.entity.exam.*;
import com.example.backend.repository.cache.ClassOfferingCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.StudentDashboardViewRepository;
import com.example.backend.repository.exam.*;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

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

    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final ClassOfferingCacheRepository classOfferingCacheRepository;
    private final ExamViolationLogRepository violationLogRepository;
    private final ExamRepository examRepository;
    private final StudentDashboardViewRepository dashboardViewRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamChoiceRepository examChoiceRepository;
    private final EssayRubricRepository essayRubricRepository;

    public StudentResponse getDashboard(String userId, String role) {

        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only students can access this dashboard.");
        }

        StudentProfileCache profile = studentProfileCacheRepository
                .findByStudentId(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found."));

        List<CurrentTermDTO> currentTerm = classOfferingCacheRepository.findCurrentTerm();

        String current_term = "";
        if (!currentTerm.isEmpty()) {
            CurrentTermDTO t = currentTerm.get(0);
            current_term = t.getTerm() + ", AY " + t.getAcademicYear();
        }

        StudentProfileDTO profileDto = buildProfile(profile, current_term);

        List<StudentUpcomingExamDTO> upcomingExams = examRepository.findPublishedAssignedExamsForStudent(profile.getStudentId());

        List<StudentResultSummaryDTO> resultSummary =
                examRepository.findReleasedUnviewedResultsForStudent(
                        profile.getStudentId(),
                        PageRequest.of(0, 4)
                );

        List<StudentViolationSummaryDTO> violations =
                violationLogRepository.findReviewedUnviewedViolationsForStudent(
                        profile.getStudentId(),
                        PageRequest.of(0, 4)
                );

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

    private StudentProfileDTO buildProfile(StudentProfileCache profile, String currentTerm) {

        return StudentProfileDTO.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .schoolId(profile.getStudentId())
                .emailAddress(profile.getEmailAddress())
                .programCode(profile.getProgramCode())
                .programName(profile.getProgramName())
                .yearLevel(profile.getYearLevel())
                .sectionName(profile.getSectionName())
                .collegeCode(profile.getCollegeCode())
                .collegeName(profile.getCollegeName())
                .currentTerm(currentTerm)
                .integrityStatus("Good Standing")
                .integritySubtitle("No unresolved major violation.")
                .profileImageUrl(null)
                .build();
    }

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

    @Transactional(readOnly = true)
    public StudentExamResultResponse getStudentExamResult(Long examId, String studentId) {

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
                        .findByAttemptAttemptIdOrderByOccurredAtAsc(attempt.getAttemptId())
                        .stream()
                        .filter(v -> v.getQuestion() != null)
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

}
