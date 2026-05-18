package com.example.backend.service.exam;

import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.exam.request.EssayRubricRequest;
import com.example.backend.dto.exam.response.EssayRubricScoreResponse;
import com.example.backend.dto.faculty.*;
import com.example.backend.dto.faculty.response.FacultyAttemptReviewResponse;
import com.example.backend.dto.faculty.response.FacultyExamDetailResponse;
import com.example.backend.dto.faculty.response.SimpleMessageResponse;
import com.example.backend.entity.exam.*;
import com.example.backend.repository.exam.*;
import com.example.backend.service.core.SystemActivityLogService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamWorkspaceService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final EssayRubricRepository essayRubricRepository;
    private final EssayRubricScoreRepository rubricScoreRepository;
    private final SystemActivityLogService activityLogService;
    private final ExamWorkspaceRepository examWorkspaceRepository;

    public ExamWorkspaceService(ExamRepository examRepository, ExamQuestionRepository examQuestionRepository, ExamAnswerRepository examAnswerRepository, ExamAttemptRepository examAttemptRepository, EssayRubricRepository essayRubricRepository, EssayRubricScoreRepository rubricScoreRepository, SystemActivityLogService activityLogService, ExamWorkspaceRepository examWorkspaceRepository) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examAnswerRepository = examAnswerRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.essayRubricRepository = essayRubricRepository;
        this.rubricScoreRepository = rubricScoreRepository;
        this.activityLogService = activityLogService;
        this.examWorkspaceRepository = examWorkspaceRepository;
    }

    @TrackActivity(
            module = "FACULTY",
            action = "GET_FACULTY_EXAM_DETAIL"
    )
    public FacultyExamDetailResponse getExamDetail(
            Long examId,
            String schoolId,
            String role
    ) {

        validateRole(role);

        FacultyExamDetailResponse response =
                examWorkspaceRepository.findExamDetail(examId, schoolId, role);

        if (response == null) {
            throw new RuntimeException("Exam not found.");
        }

        response.setNotSubmittedCount(
                response.getTotalStudents() - response.getSubmittedCount()
        );

        response.setAssignedClasses(
                examWorkspaceRepository.findExamAssignedClasses(examId, schoolId, role)
        );

        return response;
    }

    @Transactional
    public SimpleMessageResponse updateAnswerScore(
            Long answerId,
            BigDecimal pointsAwarded,
            String employeeId,
            String role
    ) {
        long start = System.currentTimeMillis();

        ExamAnswer answer = null;
        ExamAttempt attempt = null;

        try {
            validateRole(role);

            if (pointsAwarded == null) {

                long durationMs = System.currentTimeMillis() - start;

                activityLogService.log(
                        employeeId,
                        role,

                        "FACULTY_REVIEW",
                        "UPDATE_ANSWER_SCORE",

                        "FAILED",
                        "Points awarded is required.",

                        null,
                        null,
                        null,

                        durationMs
                );

                return new SimpleMessageResponse(
                        false,
                        "Points awarded is required."
                );
            }

            if (pointsAwarded.compareTo(BigDecimal.ZERO) < 0) {

                long durationMs = System.currentTimeMillis() - start;

                activityLogService.log(
                        employeeId,
                        role,

                        "FACULTY_REVIEW",
                        "UPDATE_ANSWER_SCORE",

                        "FAILED",
                        "Negative score input for answer ID " + answerId,

                        null,
                        null,
                        null,

                        durationMs
                );

                return new SimpleMessageResponse(
                        false,
                        "Points awarded cannot be negative."
                );
            }

            answer = examAnswerRepository.findById(answerId)
                    .orElseThrow(() ->
                            new RuntimeException("Answer not found.")
                    );

            attempt = answer.getAttempt();

            if (!examWorkspaceRepository.canAccessExam(
                    attempt.getExamId(),
                    employeeId,
                    role
            )) {
                throw new RuntimeException("Exam not found or access denied.");
            }

            BigDecimal maxPoints = answer.getQuestion().getPoints();

            if (maxPoints == null) {
                maxPoints = BigDecimal.ZERO;
            }

            if (pointsAwarded.compareTo(maxPoints) > 0) {

                long durationMs = System.currentTimeMillis() - start;

                activityLogService.log(
                        employeeId,
                        role,

                        "FACULTY_REVIEW",
                        "UPDATE_ANSWER_SCORE",

                        "FAILED",
                        "Score " + pointsAwarded +
                                " exceeds max " + maxPoints +
                                " for answer ID " + answerId,

                        attempt.getExamId(),
                        attempt.getAttemptId(),
                        answer.getQuestion().getQuestionId(),

                        durationMs
                );

                return new SimpleMessageResponse(
                        false,
                        "Points awarded cannot exceed " + maxPoints + "."
                );
            }

            answer.setPointsAwarded(pointsAwarded);
            answer.setIsCorrect( pointsAwarded.compareTo(BigDecimal.ZERO) > 0 );
            answer.setManuallyReviewed(true);
            answer.setNeedsChecking(false);
            answer.setReviewStatus("REVIEWED");
            answer.setUpdatedAt(OffsetDateTime.now());

            examAnswerRepository.save(answer);

            BigDecimal totalScore =
                    examAnswerRepository.sumPointsAwardedByAttemptId(
                            attempt.getAttemptId()
                    );

            BigDecimal totalPossible =
                    examQuestionRepository.sumTotalPointsByExamId(
                            attempt.getExamId()
                    );

            double percentage = 0.0;

            if (totalPossible != null &&
                    totalPossible.compareTo(BigDecimal.ZERO) > 0) {

                percentage = totalScore
                        .divide(totalPossible, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            attempt.setTotalScore(totalScore.doubleValue());
            attempt.setScorePercentage(percentage);

            examAttemptRepository.save(attempt);

            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    employeeId,
                    role,

                    "FACULTY_REVIEW",
                    "UPDATE_ANSWER_SCORE",

                    "SUCCESS",
                    "Updated answer ID " + answerId +
                            " to " + pointsAwarded + " points.",

                    attempt.getExamId(),
                    attempt.getAttemptId(),
                    answer.getQuestion().getQuestionId(),

                    durationMs
            );

            return new SimpleMessageResponse(
                    true,
                    "Answer score updated successfully."
            );

        } catch (Exception e) {

            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    employeeId,
                    role,

                    "FACULTY_REVIEW",
                    "UPDATE_ANSWER_SCORE",

                    "FAILED",
                    "Failed to update answer ID " + answerId +
                            ": " + e.getMessage(),

                    attempt == null ? null : attempt.getExamId(),
                    attempt == null ? null : attempt.getAttemptId(),
                    answer == null
                            ? null
                            : answer.getQuestion().getQuestionId(),

                    durationMs
            );

            throw e;
        }
    }

    // =========================================================
    // SUBMISSIONS
    // =========================================================

    @TrackActivity(
            module = "FACULTY",
            action = "GET_EXAM_SUBMISSIONS"
    )
    public List<FacultySubmissionSummaryDTO> getExamSubmissions(
            Long examId,
            String employeeId,
            String role
    ) {

        validateRole(role);

        return examWorkspaceRepository.findExamSubmissions(
                examId,
                employeeId,
                role
        );
    }

    @Transactional
    public SimpleMessageResponse markAttemptReviewed(
            Long attemptId,
            String employeeId,
            String role
    ) {
        long start = System.currentTimeMillis();

        ExamAttempt attempt = null;

        try {
            validateRole(role);

            attempt = examAttemptRepository.findById(attemptId)
                    .orElseThrow(() -> new RuntimeException("Attempt not found."));

            if (!examWorkspaceRepository.canAccessExam(
                    attempt.getExamId(),
                    employeeId,
                    role
            )) {
                throw new RuntimeException("Exam not found or access denied.");
            }

            attempt.setReviewStatus("REVIEWED");
            attempt.setReviewedBy(employeeId);
            attempt.setReviewedAt(OffsetDateTime.now());

            examAttemptRepository.save(attempt);

            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    employeeId,
                    role,
                    "FACULTY_REVIEW",
                    "COMPLETE_REVIEW",
                    "SUCCESS",
                    "Marked attempt as reviewed.",
                    attempt.getExamId(),
                    attempt.getAttemptId(),
                    null,
                    durationMs
            );

            return new SimpleMessageResponse(
                    true,
                    "Attempt marked as reviewed."
            );

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    employeeId,
                    role,
                    "FACULTY_REVIEW",
                    "COMPLETE_REVIEW",
                    "FAILED",
                    "Failed to mark attempt ID " + attemptId + " as reviewed: " + e.getMessage(),
                    attempt == null ? null : attempt.getExamId(),
                    attempt == null ? attemptId : attempt.getAttemptId(),
                    null,
                    durationMs
            );

            throw e;
        }
    }

    // =========================================================
    // VIOLATIONS
    // =========================================================

    @TrackActivity(
            module = "FACULTY",
            action = "GET_EXAM_VIOLATIONS"
    )
    public List<FacultyStudentViolationDTO> getExamViolations(
            Long examId,
            String employeeId,
            String role
    ) {

        validateRole(role);

        return examWorkspaceRepository.findExamViolations(
                examId,
                employeeId,
                role
        );
    }

    // =========================================================
    // STUDENTS
    // =========================================================

    @TrackActivity(
            module = "FACULTY",
            action = "GET_EXAM_STUDENTS"
    )
    public List<FacultyExamStudentDTO> getExamStudents(
            Long examId,
            String schoolId,
            String role
    ) {

        validateRole(role);

        return examWorkspaceRepository.findExamStudents(examId, schoolId, role);
    }

    @TrackActivity(
            module = "FACULTY",
            action = "GET_STUDENT_ATTEMPT_REVIEW"
    )
    public FacultyAttemptReviewResponse getStudentAttemptReview(
            Long examId,
            String studentId,
            String employeeId,
            String role
    ) {
        validateRole(role);

        List<FacultyExamStudentDTO> students =
                examWorkspaceRepository.findExamStudents(examId, employeeId, role);

        FacultyExamStudentDTO student = students.stream()
                .filter(s -> studentId.equals(s.getStudentId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Student not found or not assigned to this faculty."));

        List<FacultyAttemptAnswerReviewDTO> answers =
                examWorkspaceRepository.findAttemptAnswersForReview(examId, studentId);

        List<Long> questionIds = answers.stream()
                .map(FacultyAttemptAnswerReviewDTO::getQuestionId)
                .filter(Objects::nonNull)
                .toList();

        List<Long> answerIds = answers.stream()
                .map(FacultyAttemptAnswerReviewDTO::getAnswerId)
                .filter(Objects::nonNull)
                .toList();

        List<EssayRubric> allRubrics = questionIds.isEmpty()
                ? List.of()
                : essayRubricRepository.findByQuestionQuestionIdInOrderByQuestionQuestionIdAscDisplayOrderAsc(questionIds);

        Map<Long, List<EssayRubric>> rubricMap = allRubrics.stream()
                .collect(Collectors.groupingBy(r -> r.getQuestion().getQuestionId()));

        List<EssayRubricScore> allScores = answerIds.isEmpty()
                ? List.of()
                : rubricScoreRepository.findByAnswerAnswerIdIn(answerIds);

        Map<Long, List<EssayRubricScore>> scoreMap = allScores.stream()
                .collect(Collectors.groupingBy(s -> s.getAnswer().getAnswerId()));

        List<FacultyAttemptViolationDTO> violations =
                examWorkspaceRepository.findAttemptViolationsForReview(examId, studentId);

        Map<Long, List<FacultyAttemptViolationDTO>> violationsByQuestion =
                violations.stream()
                        .filter(v -> v.getQuestionId() != null)
                        .collect(Collectors.groupingBy(FacultyAttemptViolationDTO::getQuestionId));

        for (FacultyAttemptAnswerReviewDTO answer : answers) {
            List<EssayRubricRequest> rubricDtos =
                    rubricMap.getOrDefault(answer.getQuestionId(), List.of())
                            .stream()
                            .map(r -> {
                                EssayRubricRequest dto = new EssayRubricRequest();
                                dto.setRubricId(r.getRubricId());
                                dto.setCriterionName(r.getCriterionName());
                                dto.setWeightPercentage(r.getWeightPercentage());
                                dto.setDescription(r.getDescription());
                                dto.setDisplayOrder(r.getDisplayOrder());
                                return dto;
                            })
                            .toList();

            List<EssayRubricScoreResponse> scoreDtos =
                    scoreMap.getOrDefault(answer.getAnswerId(), List.of())
                            .stream()
                            .map(s -> new EssayRubricScoreResponse(
                                    s.getRubric().getRubricId(),
                                    s.getScorePercentage(),
                                    s.getScoreAwarded(),
                                    s.getFeedback()
                            ))
                            .toList();

            answer.setRubrics(rubricDtos);
            answer.setRubricScores(scoreDtos);
            answer.setViolations(
                    violationsByQuestion.getOrDefault(
                            answer.getQuestionId(),
                            new ArrayList<>()
                    )
            );

            if (answer.getViolations() != null &&
                    !answer.getViolations().isEmpty() &&
                    !"REVIEWED".equalsIgnoreCase(safe(answer.getReviewStatus()))) {

                answer.setNeedsChecking(true);
                answer.setReviewStatus("FLAGGED");
            }
        }

        List<FacultyAttemptViolationDTO> generalViolations =
                violations.stream()
                        .filter(v -> v.getQuestionId() == null)
                        .toList();

        boolean needsChecking =
                answers.stream().anyMatch(a ->
                        Boolean.TRUE.equals(a.getNeedsChecking())
                                || Boolean.TRUE.equals(a.getNeedsManualCheck())
                                || "PENDING".equalsIgnoreCase(safe(a.getReviewStatus()))
                                || "FLAGGED".equalsIgnoreCase(safe(a.getReviewStatus()))
                )
                        || !violations.isEmpty();

        ExamAttempt attempt = examAttemptRepository
                .findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new RuntimeException("Attempt not found."));

        return new FacultyAttemptReviewResponse(
                attempt.getAttemptId(),
                examId,
                student.getStudentId(),
                student.getStudentName(),
                student.getAttemptStatus(),
                student.getScorePercentage(),
                needsChecking,
                student.getStartedAt(),
                student.getSubmittedAt(),
                answers,
                generalViolations
        );
    }

    // =========================================================
    // LEADERBOARD
    // =========================================================

    @TrackActivity(
            module = "FACULTY",
            action = "GET_EXAM_LEADERBOARD"
    )
    public List<FacultyLeaderboardDTO> getExamLeaderboard(
            Long examId,
            String employeeId,
            String role
    ) {

        validateRole(role);

        List<FacultyLeaderboardDTO> leaderboard =
                examWorkspaceRepository.findExamLeaderboard(examId, employeeId, role);

        BigDecimal totalPossible =
                examQuestionRepository.sumTotalPointsByExamId(examId);

        double totalPossibleScore =
                totalPossible == null ? 0.0 : totalPossible.doubleValue();

        leaderboard.sort(
                Comparator
                        .comparingDouble((FacultyLeaderboardDTO row) ->
                                row.getTotalScore() == null ? 0.0 : row.getTotalScore()
                        )
                        .reversed()
                        .thenComparing(
                                FacultyLeaderboardDTO::getSubmittedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(FacultyLeaderboardDTO::getStudentId)
        );

        int rank = 0;
        Double previousScore = null;

        for (FacultyLeaderboardDTO row : leaderboard) {

            double currentScore =
                    row.getTotalScore() == null ? 0.0 : row.getTotalScore();

            if (previousScore == null ||
                    Double.compare(currentScore, previousScore) != 0) {
                rank++;
            }

            row.setRank(rank);
            row.setTotalPossibleScore(totalPossibleScore);

            previousScore = currentScore;
        }

        return leaderboard;
    }

    @Transactional
    public SimpleMessageResponse releaseExamResults(
            Long examId,
            String employeeId,
            String role
    ) {
        long start = System.currentTimeMillis();

        try {
            validateRole(role);

            if (!examWorkspaceRepository.canAccessExam(examId, employeeId, role)) {
                throw new RuntimeException("Exam not found or access denied.");
            }

            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() ->
                            new RuntimeException("Exam not found.")
                    );

            if (Boolean.TRUE.equals(exam.getResultsReleased())) {
                long durationMs = System.currentTimeMillis() - start;

                activityLogService.log(
                        employeeId,
                        role,
                        "RESULTS",
                        "RELEASE_RESULTS",
                        "FAILED",
                        "Results already released for exam ID " + examId + ".",
                        examId,
                        null,
                        null,
                        durationMs
                );

                return new SimpleMessageResponse(
                        false,
                        "Results are already released."
                );
            }

            backfillAttemptScoresBeforeRelease(examId);
            exam.setResultsReleased(true);
            exam.setResultsReleasedAt(OffsetDateTime.now());
            examRepository.save(exam);

            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    employeeId,
                    role,
                    "RESULTS",
                    "RELEASE_RESULTS",
                    "SUCCESS",
                    "Released results for exam: " + exam.getTitle(),
                    examId,
                    null,
                    null,
                    durationMs
            );

            return new SimpleMessageResponse(
                    true,
                    "Results released successfully."
            );

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    employeeId,
                    role,
                    "RESULTS",
                    "RELEASE_RESULTS",
                    "FAILED",
                    "Failed to release results for exam ID " + examId + ": " + e.getMessage(),
                    examId,
                    null,
                    null,
                    durationMs
            );

            throw e;
        }
    }

    @TrackActivity(
            module = "EXAM_WORKSPACE",
            action = "GET_ACTIVITY_LOGS"
    )
    public List<FacultyActivityLogDTO> getExamActivityLogs(
            Long examId,
            String employeeId,
            String role
    ) {
        validateRole(role);

        if (!examWorkspaceRepository.canAccessExam(examId, employeeId, role)) {
            throw new RuntimeException("Exam not found or access denied.");
        }

        List<FacultyActivityLogDTO> systemLogs =
                examWorkspaceRepository.findSystemActivityLogsByExamId(examId);

        List<FacultyActivityLogDTO> violationLogs =
                examWorkspaceRepository.findViolationActivityLogsByExamId(examId);

        List<FacultyActivityLogDTO> allLogs = new ArrayList<>();

        allLogs.addAll(systemLogs);
        allLogs.addAll(violationLogs);

        return allLogs.stream()
                .sorted((a, b) -> {
                    if (a.getOccurredAt() == null && b.getOccurredAt() == null) return 0;
                    if (a.getOccurredAt() == null) return 1;
                    if (b.getOccurredAt() == null) return -1;
                    return a.getOccurredAt().compareTo(b.getOccurredAt());
                })
                .toList();
    }


    // =========================================================
    // HELPER
    // =========================================================

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateRole(String role) {

        if (role == null) {
            throw new RuntimeException("Unauthorized access.");
        }

        boolean allowed =
                "FACULTY".equalsIgnoreCase(role) ||
                        "ADMIN".equalsIgnoreCase(role);

        if (!allowed) {
            throw new RuntimeException(
                    "Only faculty or admin users can access this resource."
            );
        }
    }

    private void backfillAttemptScoresBeforeRelease(Long examId) {

        List<ExamAttempt> attempts =
                examAttemptRepository.findByExamId(examId);

        for (ExamAttempt attempt : attempts) {

            BigDecimal totalScore =
                    examAnswerRepository.sumPointsAwardedByAttemptId(
                            attempt.getAttemptId()
                    );

            BigDecimal totalPossible =
                    examQuestionRepository.sumTotalPointsByExamId(
                            attempt.getExamId()
                    );

            if (totalScore == null) {
                totalScore = BigDecimal.ZERO;
            }

            double percentage = 0.0;

            if (totalPossible != null
                    && totalPossible.compareTo(BigDecimal.ZERO) > 0) {

                percentage = totalScore
                        .divide(totalPossible, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            attempt.setTotalScore(totalScore.doubleValue());
            attempt.setScorePercentage(percentage);

            boolean needsReview =
                    examAnswerRepository.existsPendingReviewByAttemptId(
                            attempt.getAttemptId()
                    );

            if (!needsReview
                    && !"REVIEWED".equalsIgnoreCase(attempt.getReviewStatus())) {
                attempt.setReviewStatus("REVIEWED");
                attempt.setReviewedBy("SYSTEM");
                attempt.setReviewedAt(OffsetDateTime.now());
            }

            examAttemptRepository.save(attempt);
        }
    }
}
