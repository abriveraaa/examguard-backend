package com.example.backend.service.exam;

import com.example.backend.dto.exam.request.*;
import com.example.backend.dto.exam.response.*;
import com.example.backend.dto.exam.result.ExamResult;
import com.example.backend.dto.faculty.request.ViolationDecisionRequest;
import com.example.backend.dto.exam.result.ExamTakingRawContent;
import com.example.backend.dto.faculty.response.SimpleMessageResponse;
import com.example.backend.entity.cache.ClassOfferingCache;
import com.example.backend.entity.cache.FacultyLoadCache;
import com.example.backend.entity.cache.FacultyProfileCache;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.AdminProfile;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.entity.enums.ExamAttemptStatus;
import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.enums.QuestionType;
import com.example.backend.entity.exam.*;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.exam.*;
import com.example.backend.service.core.SystemActivityLogService;
import com.example.backend.repository.cache.*;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.repository.core.UserAccessRepository;
import com.example.backend.entity.enums.ExamStatus;
import com.example.backend.service.core.EmailService;
import com.google.gson.Gson;
import net.coobird.thumbnailator.Thumbnails;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.util.stream.Stream;

import static com.example.backend.entity.enums.ExamStatus.*;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamChoiceRepository choiceRepository;
    private final ExamAssignmentRepository assignmentRepository;
    private final ExamViolationSettingRepository examViolationSettingRepository;
    private final QuestionViolationOverrideRepository questionViolationOverrideRepository;
    private final ExamStatusService examStatusService;
    private final ClassOfferingCacheRepository classOfferingCacheRepository;
    private final UserAccessRepository userAccessRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final FacultyLoadCacheRepository facultyLoadCacheRepository;
    private final ExamTemplateService examTemplateService;
    private final EmailService emailService;
    private final ExamAttemptRepository attemptRepository;
    private final ExamAttemptChoiceOrderRepository attemptChoiceOrderRepository;
    private final ExamTakingCacheService examTakingCacheService;
    private final ExamViolationLogRepository violationLogRepository;
    private final ExamAnswerRepository answerRepository;
    private final EssayRubricRepository essayRubricRepository;
    private final EssayRubricScoreRepository rubricScoreRepository;
    private final SystemActivityLogService activityLogService;
    private final Gson gson = new Gson();

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ExamAssignmentService examAssignmentService;

    public ExamService(ExamRepository examRepository,
                       ExamQuestionRepository questionRepository,
                       ExamChoiceRepository choiceRepository,
                       ExamAssignmentRepository assignmentRepository,
                       ExamViolationSettingRepository examViolationSettingRepository,
                       QuestionViolationOverrideRepository questionViolationOverrideRepository,
                       ClassOfferingCacheRepository classOfferingCacheRepository,
                       ExamStatusService examStatusService,
                       UserAccessRepository userAccessRepository,
                       AdminProfileRepository adminProfileRepository, StudentProfileCacheRepository studentProfileCacheRepository,
                       FacultyProfileCacheRepository facultyProfileCacheRepository,
                       FacultyLoadCacheRepository facultyLoadCacheRepository,
                       ExamTemplateService examTemplateService,
                       EmailService emailService,
                       ExamAttemptRepository attemptRepository,
                       ExamAttemptChoiceOrderRepository attemptChoiceOrderRepository,
                       ExamTakingCacheService examTakingCacheService,
                       ExamViolationLogRepository violationLogRepository,
                       ExamAnswerRepository answerRepository,
                       EssayRubricRepository essayRubricRepository,
                       EssayRubricScoreRepository rubricScoreRepository,
                       SystemActivityLogService activityLogService,
                       ExamAssignmentService examAssignmentService) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.assignmentRepository = assignmentRepository;
        this.examViolationSettingRepository = examViolationSettingRepository;
        this.questionViolationOverrideRepository = questionViolationOverrideRepository;
        this.classOfferingCacheRepository = classOfferingCacheRepository;
        this.examStatusService = examStatusService;
        this.userAccessRepository = userAccessRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.studentProfileCacheRepository = studentProfileCacheRepository;
        this.facultyProfileCacheRepository = facultyProfileCacheRepository;
        this.facultyLoadCacheRepository = facultyLoadCacheRepository;
        this.examTemplateService = examTemplateService;
        this.emailService = emailService;
        this.attemptRepository = attemptRepository;
        this.attemptChoiceOrderRepository = attemptChoiceOrderRepository;
        this.examTakingCacheService = examTakingCacheService;
        this.violationLogRepository = violationLogRepository;
        this.answerRepository = answerRepository;
        this.essayRubricRepository = essayRubricRepository;
        this.rubricScoreRepository = rubricScoreRepository;
        this.activityLogService = activityLogService;
        this.examAssignmentService = examAssignmentService;
    }

    @Transactional
    public ExamResult examDraft(ExamRequest request,
                                String schoolId,
                                String role) {
        return createExam(request, schoolId, role, false);
    }

    @Transactional
    public ExamResult examPublish(ExamRequest request, String schoolId, String role) {
        return createExam(request, schoolId, role, true);
    }

    @Transactional
    public ExamResult updateExam(Long examId, ExamRequest request, String schoolId, String role) {

        Exam exam = examRepository.findById(examId).orElse(null);

        if (exam == null) {
            return new ExamResult(false, "Exam not found.", null, 0);
        }

        if (exam.getStatus() == CANCELLED) {
            return new ExamResult(false, "Cancelled exam cannot be edited.", examId, 0);
        }

        if (exam.getStatus() == ExamStatus.COMPLETED) {
            return new ExamResult(false, "Completed exam cannot be edited.", examId, 0);
        }

        String validation = validateExamRequest(request, true);

        if (validation != null) {
            return new ExamResult(false, validation, examId, 0);
        }

        applyExamFields(exam, request);
        exam.setUpdatedBy(schoolId);
        exam.setUpdatedByRole(role);

        Exam savedExam = examRepository.save(exam);

        deleteExamChildren(examId);

        saveAssignments(savedExam, request.getClassOfferingIds(), schoolId, savedExam.getStatus());
        saveQuestions(savedExam, request.getQuestions());
        saveViolationSettings(savedExam, request.getViolationSettings());

        return new ExamResult(
                true,
                "Exam updated successfully.",
                savedExam.getExamId(),
                request.getQuestions() == null ? 0 : request.getQuestions().size()
        );
    }

    @Transactional
    public ExamResult publishExamById(Long examId, String schoolId, String role) {

        Exam exam = examRepository.findById(examId).orElse(null);

        if (exam == null) {
            return new ExamResult(false, "Exam not found.", null, 0);
        }

        if (exam.getStatus() == CANCELLED) {
            return new ExamResult(false, "Cancelled exam cannot be published.", examId, 0);
        }

        if (exam.getStatus() == ExamStatus.COMPLETED) {
            return new ExamResult(false, "Completed exam cannot be published.", examId, 0);
        }

        exam.setPublished(true);
        exam.setStatus(PUBLISHED);
        exam.setUpdatedBy(schoolId);
        exam.setUpdatedByRole(role);

        Exam savedExam = examRepository.save(exam);

        List<ExamAssignment> assignments =
                assignmentRepository.findByExamExamId(examId);

        for (ExamAssignment assignment : assignments) {
            assignment.setStatus(PUBLISHED);
        }

        assignmentRepository.saveAll(assignments);

        List<String> classOfferingIds = assignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .toList();

        notifyStudents(savedExam, classOfferingIds, ExamStatus.PUBLISHED);

        examTakingCacheService.warmCache(exam.getExamId());

        return new ExamResult(true, "Exam published successfully.", examId, 0);
    }

    @Transactional
    public void logViolation(ViolationLogRequest request) {
        if (request == null) {
            throw new RuntimeException("Violation request is required.");
        }

        ExamAttempt attempt = attemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new RuntimeException("Attempt not found."));

        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        ExamQuestion question = null;

        if (request.getQuestionId() != null) {
            question = questionRepository.findById(request.getQuestionId())
                    .orElse(null);
        }

        ExamViolationLog log = new ExamViolationLog();
        log.setAttempt(attempt);
        log.setExam(exam);
        log.setQuestion(question);
        log.setViolationType(request.getViolationType());
        log.setSeverity(request.getSeverity());
        log.setViolationMessage(request.getViolationMessage());
        log.setAttemptNumber(request.getAttemptNumber());
        log.setOccurredAt(
                request.getOccurredAt() == null
                        ? OffsetDateTime.now(ZoneOffset.UTC)
                        : request.getOccurredAt()
        );

        violationLogRepository.save(log);
    }

    @Transactional
    public ExamResult logExamActivity(
            ExamActivityRequest request,
            String studentId,
            String role
    ) {
        if (!"STUDENT".equalsIgnoreCase(role)) {
            return new ExamResult(false, "Only students can log exam activity.", null, 0);
        }

        if (request == null || request.getExamId() == null || request.getAttemptId() == null) {
            return new ExamResult(false, "Invalid activity request.", null, 0);
        }

        ExamAttempt attempt = attemptRepository.findById(request.getAttemptId())
                .orElse(null);

        if (attempt == null) {
            return new ExamResult(false, "Attempt not found.", null, 0);
        }

        if (!studentId.equals(attempt.getStudentId())) {
            return new ExamResult(false, "Unauthorized activity log.", null, 0);
        }

        if (!request.getExamId().equals(attempt.getExamId())) {
            return new ExamResult(false, "Exam does not match attempt.", null, 0);
        }

        activityLogService.logExamQuestionActivity(
                studentId,
                role,
                request.getExamId(),
                request.getAttemptId(),
                request.getQuestionId(),
                request.getAction(),
                request.getMessage(),
                request.getDurationMs(),
                request.getMetadata()
        );

        return new ExamResult(true, "Activity logged.", request.getExamId(), 1);
    }

    @Transactional
    public ExamResult cancelExam(Long examId, String schoolId, String role) {

        Exam exam = examRepository.findById(examId)
                .orElse(null);

        if (exam == null) {
            return new ExamResult(false, "Exam not found.", null, 0);
        }

        if (exam.getStatus() == ExamStatus.COMPLETED) {
            return new ExamResult(false, "Completed exam cannot be cancelled.", examId, 0);
        }

        ExamStatus previousStatus = exam.getStatus();

        exam.setPublished(false);
        exam.setStatus(CANCELLED);
        exam.setUpdatedBy(schoolId);
        exam.setUpdatedByRole(role);

        examRepository.save(exam);

        if (previousStatus == ExamStatus.PUBLISHED) {

            List<String> classOfferingIds =
                    assignmentRepository.findClassOfferingIdsByExamId(examId);

            notifyStudents(exam, classOfferingIds, ExamStatus.CANCELLED);
        }

        examTakingCacheService.evictCache(exam.getExamId());

        return new ExamResult(true, "Exam cancelled successfully.", examId, 0);
    }

    @Transactional
    public ExamResult restoreExam(Long examId, String schoolId, String role) {

        Exam exam = examRepository.findById(examId)
                .orElse(null);

        if (exam == null) {
            return new ExamResult(false, "Exam not found.", null, 0);
        }

        if (exam.getStatus() != CANCELLED) {
            return new ExamResult(false, "Only cancelled exams can be restored.", examId, 0);
        }

        exam.setStatus(ExamStatus.DRAFT);
        exam.setPublished(false);
        exam.setUpdatedBy(schoolId);
        exam.setUpdatedByRole(role);

        examRepository.save(exam);

        return new ExamResult(true, "Exam restored to draft successfully.", examId, 0);
    }

    @Transactional
    public ExamResult saveEssayReview(
            FacultyEssayReviewRequest request,
            String employeeId,
            String role
    ) {
        validateReviewerRole(role);

        if (request == null || request.getAnswerId() == null) {
            return new ExamResult(false, "Essay review request is invalid.", null, 0);
        }

        ExamAnswer answer = answerRepository.findById(request.getAnswerId())
                .orElseThrow(() -> new RuntimeException("Answer not found."));

        ExamQuestion question = answer.getQuestion();

        if (question.getQuestionType() != QuestionType.ESSAY) {
            return new ExamResult(false, "Only essay answers can be reviewed.", null, 0);
        }

        if (request.getRubricScores() == null || request.getRubricScores().isEmpty()) {
            return new ExamResult(false, "Rubric scores are required.", null, 0);
        }

        BigDecimal totalAwarded = BigDecimal.ZERO;

        for (EssayRubricScoreRequest scoreRequest : request.getRubricScores()) {

            EssayRubric rubric = essayRubricRepository.findById(scoreRequest.getRubricId())
                    .orElseThrow(() -> new RuntimeException("Rubric not found."));

            BigDecimal scorePercentage = scoreRequest.getScorePercentage();

            if (scorePercentage == null ||
                    scorePercentage.compareTo(BigDecimal.ZERO) < 0 ||
                    scorePercentage.compareTo(new BigDecimal("100")) > 0) {

                return new ExamResult(
                        false,
                        rubric.getCriterionName() + " score must be between 0% and 100%.",
                        null,
                        0
                );
            }

            BigDecimal criterionMaxPoints = question.getPoints()
                    .multiply(rubric.getWeightPercentage())
                    .divide(new BigDecimal("100"));

            BigDecimal awardedPoints = criterionMaxPoints
                    .multiply(scorePercentage)
                    .divide(new BigDecimal("100"));

            totalAwarded = totalAwarded.add(awardedPoints);

            EssayRubricScore row = rubricScoreRepository
                    .findByAnswerAnswerIdAndRubricRubricId(
                            answer.getAnswerId(),
                            rubric.getRubricId()
                    )
                    .orElseGet(EssayRubricScore::new);

            row.setAnswer(answer);
            row.setRubric(rubric);
            row.setScorePercentage(scorePercentage);
            row.setScoreAwarded(awardedPoints);
            row.setFeedback(scoreRequest.getFeedback());
            row.setUpdatedAt(OffsetDateTime.now());

            rubricScoreRepository.save(row);
        }

        if (totalAwarded.compareTo(question.getPoints()) > 0) {
            return new ExamResult(false, "Total score cannot exceed question points.", null, 0);
        }

        answer.setPointsAwarded(totalAwarded);
        answer.setFacultyFeedback(
                upsertFeedbackBlock(
                        answer.getFacultyFeedback(),
                        "Essay Feedback",
                        request.getFacultyFeedback()
                )
        );
        answer.setIsCorrect(null);
        answer.setNeedsChecking(false);
        answer.setReviewStatus("REVIEWED");
        answer.setManuallyReviewed(true);
        answer.setUpdatedAt(OffsetDateTime.now());

        answerRepository.save(answer);

        recomputeAttemptScore(answer.getAttempt());

        return new ExamResult(
                true,
                "Essay review saved.",
                question.getExam().getExamId(),
                1
        );
    }

    private String upsertFeedbackBlock(
            String existing,
            String blockTitle,
            String newText
    ) {
        String safeText = newText == null || newText.isBlank()
                ? "No feedback provided."
                : newText.trim();

        String newBlock = "[" + blockTitle + "]\n" + safeText;

        if (existing == null || existing.isBlank()) {
            return newBlock;
        }

        String startMarker = "[" + blockTitle + "]";
        int start = existing.indexOf(startMarker);

        if (start < 0) {
            return existing.trim() + "\n\n" + newBlock;
        }

        int nextBlock = existing.indexOf("\n\n[", start + startMarker.length());

        if (nextBlock < 0) {
            return (
                    existing.substring(0, start).trim() +
                            "\n\n" +
                            newBlock
            ).trim();
        }

        return (
                existing.substring(0, start).trim() +
                        "\n\n" +
                        newBlock +
                        "\n\n" +
                        existing.substring(nextBlock).trim()
        ).trim();
    }

    private void recomputeAttemptScore(ExamAttempt attempt) {

        BigDecimal totalScore =
                answerRepository.sumPointsAwardedByAttemptId(
                        attempt.getAttemptId()
                );

        BigDecimal totalPossible =
                questionRepository.sumTotalPointsByExamId(
                        attempt.getExamId()
                );

        if (totalScore == null) {
            totalScore = BigDecimal.ZERO;
        }

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

        attemptRepository.save(attempt);
    }

    private void autoMarkReviewedIfNoManualReviewNeeded(ExamAttempt attempt) {

        List<ExamAnswer> answers =
                answerRepository.findByAttemptAttemptId(
                        attempt.getAttemptId()
                );

        boolean needsReview = answers.stream()
                .anyMatch(answer ->
                        Boolean.TRUE.equals(answer.getNeedsChecking())
                                || "PENDING".equalsIgnoreCase(answer.getReviewStatus())
                                || "FLAGGED".equalsIgnoreCase(answer.getReviewStatus())
                );

        if (!needsReview) {
            attempt.setReviewStatus("REVIEWED");
            attempt.setReviewedAt(OffsetDateTime.now());
            attempt.setReviewedBy("SYSTEM");
            attemptRepository.save(attempt);
        }
    }

    @Transactional
    public ExamResult replaceUploadExamTemplate(Long examId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return new ExamResult(false, "File is empty.", null, 0);
        }

        Exam exam = examRepository.findById(examId).orElse(null);

        if (exam == null) {
            return new ExamResult(false, "Exam not found.", null, 0);
        }

        if (exam.getStatus() == ExamStatus.COMPLETED) {
            return new ExamResult(false, "Completed exam cannot be modified.", examId, 0);
        }

        try {
            List<QuestionRequest> parsed =
                    examTemplateService.parseTemplate(file);

            questionViolationOverrideRepository.deleteByQuestionExamExamId(examId);
            choiceRepository.deleteByQuestionExamExamId(examId);
            questionRepository.deleteByExamExamId(examId);

            saveQuestions(exam, parsed);

            return new ExamResult(
                    true,
                    "Questions replaced successfully.",
                    examId,
                    parsed == null ? 0 : parsed.size()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ExamResult(false, "Failed to process Excel file.", examId, 0);
        }
    }

    @Transactional
    public ExamResult saveAnswer(
            SaveAnswerRequest request,
            String studentId
    ) {
        ExamAttempt attempt = attemptRepository
                .findById(request.getAttemptId())
                .orElse(null);

        if (attempt == null) {
            return new ExamResult(false, "Attempt not found.", null, 0);
        }

        if (!attempt.getStudentId().equals(studentId)) {
            return new ExamResult(false, "Unauthorized attempt.", null, 0);
        }

        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            return new ExamResult(false, "Exam already submitted.", null, 0);
        }

        ExamQuestion question = questionRepository
                .findById(request.getQuestionId())
                .orElse(null);

        if (question == null) {
            return new ExamResult(false, "Question not found.", null, 0);
        }

        if (!question.getExam().getExamId().equals(attempt.getExamId())) {
            return new ExamResult(false, "Question does not belong to this exam.", null, 0);
        }

        boolean hasViolation =
                violationLogRepository
                        .existsByAttemptAttemptIdAndQuestionQuestionId(
                                attempt.getAttemptId(),
                                question.getQuestionId()
                        );

        ExamAnswer answer = answerRepository
                .findByAttemptAttemptIdAndQuestionQuestionId(
                        request.getAttemptId(),
                        request.getQuestionId()
                )
                .orElseGet(() -> {
                    ExamAnswer newAnswer = new ExamAnswer();
                    newAnswer.setAttempt(attempt);
                    newAnswer.setQuestion(question);
                    newAnswer.setCreatedAt(OffsetDateTime.now());
                    return newAnswer;
                });

        OffsetDateTime now = OffsetDateTime.now();

        answer.setAnsweredAt(now);
        answer.setUpdatedAt(now);

        answer.setSelectedChoiceId(null);
        answer.setAnswerText("");
        answer.setIsCorrect(null);
        answer.setPointsAwarded(BigDecimal.ZERO);

        if (request.getSelectedChoiceId() != null) {

            ExamChoice choice = choiceRepository
                    .findById(request.getSelectedChoiceId())
                    .orElse(null);

            if (choice == null) {
                return new ExamResult(false, "Choice not found.", null, 0);
            }

            if (!choice.getQuestion().getQuestionId().equals(question.getQuestionId())) {
                return new ExamResult(false, "Choice does not belong to this question.", null, 0);
            }

            answer.setSelectedChoiceId(choice);
            answer.setAnswerText(choice.getChoiceText() == null ? "" : choice.getChoiceText());

            boolean correct = Boolean.TRUE.equals(choice.getCorrect());

            answer.setIsCorrect(correct);
            answer.setPointsAwarded(correct ? question.getPoints() : BigDecimal.ZERO);

            applyAutoCheckedOrFlaggedStatus(answer, hasViolation);

        } else {

            String answerText = safe(request.getAnswerText());
            String correctAnswer = safe(question.getCorrectAnswer());

            answer.setAnswerText(answerText);

            if (question.getQuestionType() == QuestionType.TRUE_FALSE) {

                boolean correct = correctAnswer.equalsIgnoreCase(answerText);

                answer.setIsCorrect(correct);
                answer.setPointsAwarded(correct ? question.getPoints() : BigDecimal.ZERO);

                applyAutoCheckedOrFlaggedStatus(answer, hasViolation);

            } else if (question.getQuestionType() == QuestionType.IDENTIFICATION) {

                boolean correct = isIdentificationAnswerCorrect(answerText, correctAnswer);

                answer.setIsCorrect(correct);
                answer.setPointsAwarded(correct ? question.getPoints() : BigDecimal.ZERO);

                applyAutoCheckedOrFlaggedStatus(answer, hasViolation);

            } else if (question.getQuestionType() == QuestionType.ESSAY) {

                answer.setIsCorrect(null);
                answer.setPointsAwarded(BigDecimal.ZERO);

                answer.setNeedsChecking(true);
                answer.setReviewStatus("PENDING");
                answer.setManuallyReviewed(false);
            }
        }

        answerRepository.save(answer);

        return new ExamResult(
                true,
                "Answer saved.",
                attempt.getExamId(),
                1
        );
    }

    private void validateReviewerRole(String role) {
        if (!"FACULTY".equalsIgnoreCase(role)
                && !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only faculty or admin can review answers.");
        }
    }

    private void applyAutoCheckedOrFlaggedStatus(
            ExamAnswer answer,
            boolean hasViolation
    ) {
        if (hasViolation) {
            answer.setNeedsChecking(true);
            answer.setReviewStatus("FLAGGED");
        } else {
            answer.setNeedsChecking(false);
            answer.setReviewStatus("AUTO_CHECKED");
        }
    }

    @Transactional
    public ExamResult submitExam(Long attemptId, String studentId) {

        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElse(null);

        if (attempt == null) {
            return new ExamResult(false, "Attempt not found.", null, 0);
        }

        List<ExamQuestion> questions =
                questionRepository.findByExamExamIdOrderByQuestionOrderAsc(
                        attempt.getExamId()
                );

        if (!attempt.getStudentId().equals(studentId)) {
            return new ExamResult(false, "Unauthorized attempt.", null, 0);
        }

        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            return new ExamResult(false, "Exam already submitted.", attempt.getExamId(), 0);
        }

        ensureAllQuestionsHaveAnswers(attempt, questions);
        attempt.setStatus(ExamAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(OffsetDateTime.now());

        attemptRepository.save(attempt);

        recomputeAttemptScore(attempt);

        autoMarkReviewedIfNoManualReviewNeeded(attempt);

        return new ExamResult(
                true,
                "Exam submitted successfully.",
                attempt.getExamId(),
                1
        );
    }

    private void ensureAllQuestionsHaveAnswers(
            ExamAttempt attempt,
            List<ExamQuestion> questions
    ) {

        for (ExamQuestion question : questions) {

            boolean exists = answerRepository
                    .existsByAttemptAttemptIdAndQuestionQuestionId(
                            attempt.getAttemptId(),
                            question.getQuestionId()
                    );

            if (exists) {
                continue;
            }

            ExamAnswer answer = new ExamAnswer();

            answer.setAttempt(attempt);
            answer.setQuestion(question);

            answer.setCreatedAt(OffsetDateTime.now());
            answer.setAnsweredAt(OffsetDateTime.now());
            answer.setUpdatedAt(OffsetDateTime.now());

            answer.setAnswerText("");
            answer.setPointsAwarded(BigDecimal.ZERO);

            if (question.getQuestionType() == QuestionType.ESSAY) {
                answer.setIsCorrect(null);
                answer.setNeedsChecking(true);
                answer.setReviewStatus("PENDING");
                answer.setManuallyReviewed(false);
            } else {
                answer.setIsCorrect(false);
                answer.setNeedsChecking(false);
                answer.setReviewStatus("AUTO_CHECKED");
                answer.setManuallyReviewed(false);
            }

            answerRepository.save(answer);
        }
    }

    private void saveViolationSettings(Exam exam, List<ViolationSettingRequest> settings) {
        examViolationSettingRepository.deleteByExamExamId(exam.getExamId());

        if (settings == null || settings.isEmpty()) {
            return;
        }

        List<ExamViolationSetting> rows = new ArrayList<>();

        for (ViolationSettingRequest request : settings) {
            if (request == null || request.getViolationType() == null) {
                continue;
            }

            ExamViolationSetting setting = new ExamViolationSetting();
            setting.setExam(exam);
            setting.setViolationType(request.getViolationType());
            setting.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
            setting.setSeverity(
                    request.getSeverity() == null || request.getSeverity().isBlank()
                            ? "MINOR"
                            : request.getSeverity()
            );
            setting.setMaxAllowedCount(
                    request.getMaxAllowedCount() == null
                            ? 0
                            : request.getMaxAllowedCount()
            );

            rows.add(setting);
        }

        examViolationSettingRepository.saveAll(rows);
    }

    private ExamResult createExam(
            ExamRequest request,
            String schoolId,
            String role,
            boolean publishNow
    ) {
        String validation = validateExamRequest(request, false);

        if (validation != null) {
            return new ExamResult(false, validation, null, 0);
        }

        Exam exam = new Exam();

        applyExamFields(exam, request);
        exam.setCreatedBy(schoolId);
        exam.setCreatedByRole(role);
        exam.setUpdatedBy(schoolId);
        exam.setUpdatedByRole(role);
        exam.setPublished(publishNow);
        exam.setStatus(publishNow ? PUBLISHED : ExamStatus.DRAFT);

        Exam savedExam = examRepository.save(exam);

        saveAssignments(savedExam, request.getClassOfferingIds(), schoolId, savedExam.getStatus());

        saveQuestions(savedExam, request.getQuestions());

        saveViolationSettings(savedExam, request.getViolationSettings());

        if (publishNow) {
            notifyStudents(savedExam, request.getClassOfferingIds(), ExamStatus.PUBLISHED);
            examTakingCacheService.warmCache(exam.getExamId());
        }

        return new ExamResult(
                true,
                publishNow ? "Exam published successfully." : "Exam saved as draft.",
                savedExam.getExamId(),
                request.getQuestions() == null ? 0 : request.getQuestions().size()
        );
    }

    public List<ExamResponse> getAllExams(String role, String schoolId) {
        long start = System.currentTimeMillis();

        List<Exam> exams;

        if ("ADMIN".equalsIgnoreCase(role)) {
            exams = examRepository.findAll();

        } else if ("FACULTY".equalsIgnoreCase(role)) {
            exams = examRepository.findVisibleExamsForFaculty(schoolId);

        } else if ("STUDENT".equalsIgnoreCase(role)) {
            exams = examRepository.findAvailableExamsForStudent(schoolId);

        } else {
            return List.of();
        }

        if (exams.isEmpty()) {
            return List.of();
        }

        List<Long> examIds = exams.stream()
                .map(Exam::getExamId)
                .toList();

        List<ExamAssignment> allAssignments =
                assignmentRepository.findByExamExamIdIn(examIds);

        Map<Long, List<ExamAssignment>> assignmentMap =
                allAssignments.stream()
                        .collect(Collectors.groupingBy(a -> a.getExam().getExamId()));

        List<Object[]> takerRows =
                examRepository.countTakersByExamIds(examIds);

        Map<Long, Integer> takerMap = new HashMap<>();

        for (Object[] row : takerRows) {
            Long rowExamId = (Long) row[0];
            Number count = (Number) row[1];
            takerMap.put(rowExamId, count.intValue());
        }

        List<Object[]> submittedRows =
                examRepository.countSubmittedTakersByExamIds(examIds);

        Map<Long, Integer> submittedMap = new HashMap<>();

        for (Object[] row : submittedRows) {
            Long rowExamId = (Long) row[0];
            Number count = (Number) row[1];
            submittedMap.put(rowExamId, count.intValue());
        }

        Map<String, String> userDisplayMap =
                buildUserDisplayMap(
                        exams.stream()
                                .flatMap(e -> Stream.of(
                                        e.getCreatedBy(),
                                        e.getUpdatedBy()
                                ))
                                .filter(Objects::nonNull)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toSet())
                );

        Set<String> classOfferingIds = allAssignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> classOfferingDisplayMap =
                buildClassOfferingDisplayMap(classOfferingIds);

        long mapStart = System.currentTimeMillis();

        List<ExamResponse> responses = exams.stream()
                .map(exam -> {
                    List<ExamAssignment> assignments =
                            assignmentMap.getOrDefault(exam.getExamId(), List.of());

                    int totalTakers = takerMap.getOrDefault(exam.getExamId(), 0);
                    int submittedTakers = submittedMap.getOrDefault(exam.getExamId(), 0);

                    return new ExamResponse(
                            exam.getExamId(),
                            exam.getTitle(),
                            exam.getDescription(),
                            formatDate(exam.getCreatedAt().toLocalDateTime()),
                            getValidUntil(assignments),
                            determineStatus(exam, assignments),
                            formatDuration(exam.getTimeLimitMinutes()),

                            formatAssignedTo(assignments, classOfferingDisplayMap),

                            formatTakers(submittedTakers, totalTakers),
                            formatDateTime(exam.getStartDateTime()),
                            formatDateTime(exam.getEndDateTime()),
                            exam.getExamMode() == null ? "" : exam.getExamMode().name(),

                            userDisplayMap.getOrDefault(exam.getCreatedBy(), exam.getCreatedBy()),
                            userDisplayMap.getOrDefault(exam.getUpdatedBy(), exam.getUpdatedBy())
                    );
                })
                .toList();

        return responses;
    }

    private Map<String, String> buildClassOfferingDisplayMap(Set<String> classOfferingIds) {
        if (classOfferingIds == null || classOfferingIds.isEmpty()) {
            return Map.of();
        }

        return classOfferingCacheRepository.findAllById(classOfferingIds)
                .stream()
                .collect(Collectors.toMap(
                        ClassOfferingCache::getClassOfferingId,
                        ClassOfferingCache::getProgramCode,
                        (existing, duplicate) -> existing
                ));
    }

    private Map<String, String> buildUserDisplayMap(Set<String> schoolIdsOrEmployeeIds) {
        if (schoolIdsOrEmployeeIds == null || schoolIdsOrEmployeeIds.isEmpty()) {
            return Map.of();
        }

        Set<String> ids = schoolIdsOrEmployeeIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, AdminProfile> adminProfileMap =
                adminProfileRepository.findByEmployeeIdIn(ids)
                        .stream()
                        .collect(Collectors.toMap(
                                AdminProfile::getEmployeeId,
                                p -> p,
                                (existing, duplicate) -> existing
                        ));

        Map<String, FacultyProfileCache> facultyProfileMap =
                facultyProfileCacheRepository.findByEmployeeIdIn(ids)
                        .stream()
                        .collect(Collectors.toMap(
                                FacultyProfileCache::getEmployeeId,
                                p -> p,
                                (existing, duplicate) -> existing
                        ));

        Map<String, String> result = new HashMap<>();

        for (String id : ids) {
            AdminProfile admin = adminProfileMap.get(id);

            if (admin != null) {
                result.put(
                        id,
                        admin.getFirstName() + " " + admin.getLastName() + " (Admin)"
                );
                continue;
            }

            FacultyProfileCache faculty = facultyProfileMap.get(id);

            if (faculty != null) {
                result.put(
                        id,
                        faculty.getFirstName() + " " + faculty.getLastName() + " (Faculty)"
                );
                continue;
            }

            result.put(id, id);
        }

        return result;
    }

    public ExamResponse viewExam(Long examId, String userId, String role) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        List<ExamAssignment> assignments =
                assignmentRepository.findByExamExamId(examId);

        List<String> classOfferingIds = assignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .distinct()
                .toList();

        List<ExamQuestion> questions =
                questionRepository.findByExamExamIdOrderByQuestionOrderAsc(examId);

        List<Long> questionIds = questions.stream()
                .map(ExamQuestion::getQuestionId)
                .toList();

        List<ExamChoice> allChoices = questionIds.isEmpty()
                ? List.of()
                : choiceRepository.findByQuestionQuestionIdInOrderByQuestionQuestionIdAscChoiceOrderAsc(questionIds);

        Map<Long, List<ExamChoice>> choiceMap = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuestion().getQuestionId()));

        List<EssayRubric> allRubrics = questionIds.isEmpty()
                ? List.of()
                : essayRubricRepository.findByQuestionQuestionIdInOrderByQuestionQuestionIdAscDisplayOrderAsc(questionIds);

        Map<Long, List<EssayRubric>> rubricMap = allRubrics.stream()
                .collect(Collectors.groupingBy(r -> r.getQuestion().getQuestionId()));

        List<ExamResponse.QuestionPreview> questionPreviews = new ArrayList<>();

        for (ExamQuestion q : questions) {
            List<ExamChoice> choices =
                    choiceMap.getOrDefault(q.getQuestionId(), List.of());

            List<ExamResponse.ChoicePreview> choicePreviews = choices.stream()
                    .map(c -> new ExamResponse.ChoicePreview(
                            c.getChoiceId(),
                            c.getChoiceLabel(),
                            c.getChoiceText(),
                            c.getChoiceImageUrl(),
                            c.getCorrect()
                    ))
                    .toList();

            List<ExamResponse.EssayRubricResponse> rubricResponses =
                    rubricMap.getOrDefault(q.getQuestionId(), List.of())
                            .stream()
                            .map(r -> new ExamResponse.EssayRubricResponse(
                                    r.getRubricId(),
                                    r.getCriterionName(),
                                    r.getWeightPercentage(),
                                    r.getDescription(),
                                    r.getDisplayOrder()
                            ))
                            .toList();

            questionPreviews.add(new ExamResponse.QuestionPreview(
                    q.getQuestionId(),
                    q.getQuestionType(),
                    q.getQuestionText(),
                    q.getQuestionImageUrl(),
                    q.getPoints(),
                    q.getQuestionOrder(),
                    q.getCorrectAnswer(),
                    choicePreviews,
                    q.getQuestionInstruction(),
                    rubricResponses
            ));
        }

        Integer totalTakers = examRepository.countTakersByExamId(exam.getExamId());
        Integer submittedTakers = examRepository.countSubmittedTakersByExamId(exam.getExamId());

        Set<String> offeringIdSet = new HashSet<>(classOfferingIds);
        Map<String, String> classOfferingDisplayMap =
                buildClassOfferingDisplayMap(offeringIdSet);

        Map<String, String> userDisplayMap =
                buildUserDisplayMap(
                        Stream.of(exam.getCreatedBy(), exam.getUpdatedBy())
                                .filter(Objects::nonNull)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toSet())
                );

        ExamResponse response = new ExamResponse(
                exam.getExamId(),
                exam.getTitle(),
                exam.getDescription(),
                formatDate(exam.getCreatedAt().toLocalDateTime()),
                getValidUntil(assignments),
                determineStatus(exam, assignments),
                formatDuration(exam.getTimeLimitMinutes()),
                formatAssignedTo(assignments, classOfferingDisplayMap),
                formatTakers(submittedTakers, totalTakers),
                formatDateTime(exam.getStartDateTime()),
                formatDateTime(exam.getEndDateTime()),
                exam.getExamMode() == null ? "" : exam.getExamMode().name(),
                userDisplayMap.getOrDefault(exam.getCreatedBy(), exam.getCreatedBy()),
                userDisplayMap.getOrDefault(exam.getUpdatedBy(), exam.getUpdatedBy()),
                questionPreviews
        );

        response.setClassOfferingIds(classOfferingIds);
        response.setTimeLimitMinutes(exam.getTimeLimitMinutes());
        response.setShuffleQuestions(exam.getShuffleQuestions());
        response.setShuffleChoices(exam.getShuffleChoices());
        response.setRawStartDateTime(exam.getStartDateTime());
        response.setRawEndDateTime(exam.getEndDateTime());

        return response;
    }

    public List<ClassOfferingResponse> getClassOfferings(String schoolId, String role) {

        List<ClassOfferingCache> offerings;

        if ("ADMIN".equalsIgnoreCase(role)) {
            offerings = classOfferingCacheRepository.findAll();

        } else if ("FACULTY".equalsIgnoreCase(role)) {

            List<String> ids = facultyLoadCacheRepository
                    .findByEmployeeId(schoolId)
                    .stream()
                    .map(FacultyLoadCache::getClassOfferingId)
                    .distinct()
                    .toList();

            offerings = classOfferingCacheRepository.findByClassOfferingIdIn(ids);

        } else {
            throw new RuntimeException("Invalid role");
        }

        return offerings.stream()
                .map(this::mapToDto)
                .toList();
    }

    private ClassOfferingResponse mapToDto(ClassOfferingCache co) {

        ClassOfferingResponse dto = new ClassOfferingResponse();

        dto.setClassOfferingId(co.getClassOfferingId());
        dto.setProgramCode(co.getProgramCode());
        dto.setYearLevel(co.getYearLevel());
        dto.setSectionName(co.getSectionName());
        dto.setCourseCode(co.getCourseCode());
        dto.setCourseDescription(co.getCourseDescription());

        return dto;
    }

    public ImageUploadResponse uploadExamImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ImageUploadResponse(false, "Image file is empty.", null);
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            return new ImageUploadResponse(false, "Only image files are allowed.", null);
        }

        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/exams/";

            File directory = new File(uploadDir);

            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    return new ImageUploadResponse(false, "Failed to create upload directory.", null);
                }
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = UUID.randomUUID() + extension;

            File destination = new File(directory, filename);

            Thumbnails.of(file.getInputStream())
                    .size(1200, 1200)
                    .outputQuality(0.85)
                    .toFile(destination);

            String imageUrl = "/uploads/exams/" + filename;

            return new ImageUploadResponse(true, "Image uploaded successfully.", imageUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return new ImageUploadResponse(false, "Failed to save image.", null);
        }
    }

    public ExamTakingResponse getExamForTaking(
            Long examId,
            String schoolId,
            String role
    ) {
        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only students can take exams.");
        }

        ExamTakingRawContent rawContent =
                examTakingCacheService.getRawContent(examId);

        Exam exam = rawContent.getExam();
        List<ExamQuestion> dbQuestions = rawContent.getQuestions();
        Map<Long, List<ExamChoice>> choiceMap = rawContent.getChoiceMap();

        Optional<ExamAttempt> existingAttempt =
                attemptRepository.findByExamIdAndStudentId(examId, schoolId);

        ExamAttempt attempt;

        if (existingAttempt.isPresent()) {
            attempt = existingAttempt.get();

            if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
                throw new RuntimeException("This exam attempt is already submitted.");
            }

        } else {
            attempt = createNewAttempt(
                    exam,
                    schoolId,
                    dbQuestions,
                    choiceMap
            );
        }

        List<Long> savedQuestionOrder = fromJsonList(attempt.getQuestionOrder());

        List<ExamQuestion> finalQuestions =
                applyQuestionOrder(dbQuestions, savedQuestionOrder);

        Map<Long, List<Long>> savedChoiceOrderMap =
                attemptChoiceOrderRepository.findByAttemptId(attempt.getAttemptId())
                        .stream()
                        .collect(Collectors.toMap(
                                ExamAttemptChoiceOrder::getQuestionId,
                                item -> fromJsonList(item.getChoiceOrder())
                        ));

        List<Long> questionIds = dbQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .toList();

        List<EssayRubric> allRubrics = questionIds.isEmpty()
                ? List.of()
                : essayRubricRepository.findByQuestionQuestionIdInOrderByQuestionQuestionIdAscDisplayOrderAsc(questionIds);

        Map<Long, List<EssayRubric>> rubricMap = allRubrics.stream()
                .collect(Collectors.groupingBy(r -> r.getQuestion().getQuestionId()));

        List<ExamTakingQuestionResponse> questionResponses = finalQuestions.stream()
                .map(question -> {

                    List<ExamChoice> choices = new ArrayList<>(
                            choiceMap.getOrDefault(
                                    question.getQuestionId(),
                                    new ArrayList<>()
                            )
                    );

                    List<Long> savedChoiceOrder =
                            savedChoiceOrderMap.get(question.getQuestionId());

                    if (savedChoiceOrder != null) {
                        choices = applyChoiceOrder(choices, savedChoiceOrder);
                    }

                    List<ExamTakingChoiceResponse> choiceResponses = new ArrayList<>();

                    char label = 'A';

                    for (ExamChoice choice : choices) {
                        choiceResponses.add(new ExamTakingChoiceResponse(
                                choice.getChoiceId(),
                                String.valueOf(label++),
                                choice.getChoiceText(),
                                choice.getChoiceOrder(),
                                choice.getChoiceImageUrl()
                        ));
                    }

                    return new ExamTakingQuestionResponse(
                            question.getQuestionId(),
                            question.getQuestionType(),
                            question.getQuestionText(),
                            question.getQuestionImageUrl(),
                            question.getPoints(),
                            choiceResponses,
                            question.getQuestionInstruction(),
                            mapRubricsForTaking(
                                    rubricMap.getOrDefault(
                                            question.getQuestionId(),
                                            List.of()
                                    )
                            )
                    );
                })
                .toList();

        List<ExamViolationSetting> dbViolationSettings =
                examViolationSettingRepository.findByExamExamId(examId);

        List<ViolationSettingRequest> violationSettings =
                dbViolationSettings.stream()
                        .map(setting -> {
                            ViolationSettingRequest dto = new ViolationSettingRequest();
                            dto.setViolationType(setting.getViolationType());
                            dto.setEnabled(setting.getEnabled());
                            dto.setSeverity(setting.getSeverity());
                            dto.setMaxAllowedCount(setting.getMaxAllowedCount());
                            return dto;
                        })
                        .toList();

        return new ExamTakingResponse(
                attempt.getAttemptId(),
                exam.getExamId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getTimeLimitMinutes(),
                exam.getStartDateTime(),
                exam.getEndDateTime(),
                exam.getExamMode(),
                questionResponses,
                violationSettings
        );
    }

    private List<EssayRubricRequest> mapRubricsForTaking(List<EssayRubric> rubrics) {
        if (rubrics == null || rubrics.isEmpty()) {
            return List.of();
        }

        return rubrics.stream()
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
    }

    // VALIDATION

    private String validateExamRequest(ExamRequest request, boolean editMode) {

        OffsetDateTime minimumStart = OffsetDateTime.now().plusHours(1);

        if (request == null) {
            return "Invalid exam request.";
        }

        if (isBlank(request.getTitle())) {
            return "Exam title is required.";
        }

        if (request.getTimeLimitMinutes() != null && request.getTimeLimitMinutes() <= 0) {
            return "Time limit must be greater than zero.";
        }

        if (request.getStartDateTime() == null) {
            throw new RuntimeException("Start date and time is required.");
        }

        if (request.getEndDateTime() == null) {
            throw new RuntimeException("End date and time is required.");
        }

        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new RuntimeException("End date and time must be after start date and time.");
        }

        if (!editMode && request.getStartDateTime().isBefore(minimumStart)) {
            return "Exam start time must be at least 1 hour from now.";
        }

        if (request.getExamMode() == null || request.getExamMode().isBlank()) {
            throw new RuntimeException("Exam mode is required.");
        }

        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            return "At least one question is required.";
        }

        try {
            ExamMode.valueOf(request.getExamMode().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid exam mode. Use SYNCHRONOUS or ASYNCHRONOUS.");
        }

        int index = 1;

        for (QuestionRequest q : request.getQuestions()) {
            String error = validateQuestion(q, index);
            if (error != null) {
                return error;
            }
            index++;
        }

        return null;
    }

    private String validateQuestion(QuestionRequest q, int index) {

        if (q == null) {
            return "Question " + index + " is invalid.";
        }

        if (isBlank(q.getQuestionType())) {
            return "Question " + index + " type is required.";
        }

        String type = q.getQuestionType().trim().toUpperCase();

        if (!isSupportedQuestionType(type)) {
            return "Question " + index + " has unsupported type: " + q.getQuestionType();
        }

        if (isBlank(q.getQuestionText()) && isBlank(q.getQuestionImageUrl())) {
            return "Question " + index + " must have text or image.";
        }

        if (q.getPoints() != null && q.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
            return "Question " + index + " points must be greater than zero.";
        }

        if ("MULTIPLE_CHOICE".equals(type)) {
            return validateMultipleChoice(q, index);
        }

        if ("TRUE_FALSE".equals(type)) {
            return validateTrueFalse(q, index);
        }

        if ("IDENTIFICATION".equals(type)) {
            return validateIdentification(q, index);
        }

        if ("ESSAY".equals(type)) {
            return validateEssay(q, index);
        }

        return null;
    }

    private String validateMultipleChoice(QuestionRequest q, int index) {

        List<ChoiceRequest> choices = q.getChoices();

        if (choices == null || choices.size() < 2) {
            return "Question " + index + " multiple choice must have at least 2 choices.";
        }

        int correctCount = 0;
        int choiceIndex = 1;

        for (ChoiceRequest c : choices) {
            if (c == null) {
                return "Question " + index + " choice " + choiceIndex + " is invalid.";
            }

            if (isBlank(c.getChoiceText()) && isBlank(c.getChoiceImageUrl())) {
                return "Question " + index + " choice " + choiceIndex + " must have text or image.";
            }

            if (Boolean.TRUE.equals(c.getCorrect())) {
                correctCount++;
            }

            choiceIndex++;
        }

        if (correctCount != 1) {
            return "Question " + index + " multiple choice must have exactly 1 correct answer.";
        }

        return null;
    }

    private String validateTrueFalse(QuestionRequest q, int index) {

        if (isBlank(q.getCorrectAnswer())) {
            return "Question " + index + " true/false must have correct answer.";
        }

        String answer = q.getCorrectAnswer().trim().toUpperCase();

        if (!"TRUE".equals(answer) && !"FALSE".equals(answer)) {
            return "Question " + index + " true/false answer must be TRUE or FALSE.";
        }

        return null;
    }

    private String validateIdentification(QuestionRequest q, int index) {

        if (isBlank(q.getCorrectAnswer())) {
            return "Question " + index + " identification must have correct answer.";
        }

        return null;
    }

    private String validateEssay(QuestionRequest q, int index) {
        return null;
    }

    private boolean isSupportedQuestionType(String type) {
        return "MULTIPLE_CHOICE".equals(type)
                || "TRUE_FALSE".equals(type)
                || "IDENTIFICATION".equals(type)
                || "ESSAY".equals(type);
    }

    // HELPER

    private void notifyStudents(
            Exam exam,
            List<String> classOfferingIds,
            ExamStatus type
    ) {
        if (classOfferingIds == null || classOfferingIds.isEmpty()) {
            return;
        }

        for (String classOfferingId : classOfferingIds) {

            ClassOfferingCache offering =
                    classOfferingCacheRepository.findById(classOfferingId).orElse(null);

            String courseCode = offering != null
                    ? offering.getCourseCode()
                    : "N/A";

            List<StudentProfileCache> students =
                    studentProfileCacheRepository.findEnrolledStudentsByClassOfferingId(classOfferingId);

            for (StudentProfileCache student : students) {

                String email = student.getEmailAddress();
                if (email == null || email.isBlank()) continue;

                switch (type) {

                    case PUBLISHED -> emailService.sendExamPublishedEmail(
                            email,
                            student.getFirstName(),
                            exam.getTitle(),
                            courseCode,
                            formatDateTime(exam.getStartDateTime()),
                            formatDateTime(exam.getEndDateTime()),
                            exam.getTimeLimitMinutes()
                    );

                    case CANCELLED -> emailService.sendExamCancelledEmail(
                            email,
                            student.getFirstName(),
                            exam.getTitle(),
                            courseCode
                    );

                    case RESULTS_RELEASED -> emailService.sendExamResultsReleasedEmail(
                            email,
                            student.getFirstName(),
                            exam.getTitle(),
                            courseCode
                    );
                }
            }
        }
    }

    private ExamAttempt createNewAttempt(
            Exam exam,
            String username,
            List<ExamQuestion> dbQuestions,
            Map<Long, List<ExamChoice>> choiceMap
    ) {
        List<ExamQuestion> normalQuestions = new ArrayList<>();
        List<ExamQuestion> essayQuestions = new ArrayList<>();

        for (ExamQuestion question : dbQuestions) {
            if (question.getQuestionType() == QuestionType.ESSAY) {
                essayQuestions.add(question);
            } else {
                normalQuestions.add(question);
            }
        }

        if (Boolean.TRUE.equals(exam.getShuffleQuestions())) {
            Collections.shuffle(normalQuestions);
            Collections.shuffle(essayQuestions);
        }

        List<ExamQuestion> finalQuestions = new ArrayList<>();
        finalQuestions.addAll(normalQuestions);
        finalQuestions.addAll(essayQuestions);

        List<Long> finalQuestionIds = finalQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .toList();

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamId(exam.getExamId());
        attempt.setStudentId(username);
        attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setQuestionOrder(toJsonList(finalQuestionIds));

        attempt = attemptRepository.save(attempt);

        List<ExamAttemptChoiceOrder> choiceOrdersToSave = new ArrayList<>();

        for (ExamQuestion question : finalQuestions) {
            List<ExamChoice> choices = new ArrayList<>(
                    choiceMap.getOrDefault(
                            question.getQuestionId(),
                            new ArrayList<>()
                    )
            );

            List<ExamChoice> normalChoices = new ArrayList<>();
            List<ExamChoice> noneOfTheAboveChoices = new ArrayList<>();

            for (ExamChoice choice : choices) {
                String text = choice.getChoiceText();

                if (text != null && text.trim().equalsIgnoreCase("None of the above")) {
                    noneOfTheAboveChoices.add(choice);
                } else {
                    normalChoices.add(choice);
                }
            }

            if (Boolean.TRUE.equals(exam.getShuffleChoices())) {
                Collections.shuffle(normalChoices);
            }

            List<ExamChoice> finalChoices = new ArrayList<>();
            finalChoices.addAll(normalChoices);
            finalChoices.addAll(noneOfTheAboveChoices);

            List<Long> finalChoiceIds = finalChoices.stream()
                    .map(ExamChoice::getChoiceId)
                    .toList();

            ExamAttemptChoiceOrder order = new ExamAttemptChoiceOrder();
            order.setAttemptId(attempt.getAttemptId());
            order.setQuestionId(question.getQuestionId());
            order.setChoiceOrder(toJsonList(finalChoiceIds));

            choiceOrdersToSave.add(order);
        }

        attemptChoiceOrderRepository.saveAll(choiceOrdersToSave);

        return attempt;
    }

    private String toJsonList(List<Long> ids) {
        return gson.toJson(ids);
    }

    private List<Long> fromJsonList(String json) {
        Long[] array = gson.fromJson(json, Long[].class);
        return new ArrayList<>(List.of(array));
    }

    private List<ExamQuestion> applyQuestionOrder(
            List<ExamQuestion> questions,
            List<Long> orderedIds
    ) {
        Map<Long, ExamQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        ExamQuestion::getQuestionId,
                        q -> q
                ));

        List<ExamQuestion> ordered = new ArrayList<>();

        for (Long id : orderedIds) {
            ExamQuestion question = questionMap.get(id);
            if (question != null) {
                ordered.add(question);
            }
        }

        return ordered;
    }

    private List<ExamChoice> applyChoiceOrder(
            List<ExamChoice> choices,
            List<Long> orderedIds
    ) {
        Map<Long, ExamChoice> choiceMap = choices.stream()
                .collect(Collectors.toMap(
                        ExamChoice::getChoiceId,
                        c -> c
                ));

        List<ExamChoice> ordered = new ArrayList<>();

        for (Long id : orderedIds) {
            ExamChoice choice = choiceMap.get(id);
            if (choice != null) {
                ordered.add(choice);
            }
        }

        return ordered;
    }

    private void applyExamFields(Exam exam, ExamRequest request) {
        exam.setTitle(request.getTitle().trim());
        exam.setDescription(request.getDescription());
        exam.setTimeLimitMinutes(request.getTimeLimitMinutes());
        exam.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));
        exam.setShuffleChoices(Boolean.TRUE.equals(request.getShuffleChoices()));
        exam.setStartDateTime(request.getStartDateTime());
        exam.setEndDateTime(request.getEndDateTime());
        exam.setExamMode(ExamMode.valueOf(request.getExamMode().trim().toUpperCase()));
    }

    private void deleteExamChildren(Long examId) {
        questionViolationOverrideRepository.deleteByQuestionExamExamId(examId);
        choiceRepository.deleteByQuestionExamExamId(examId);
        essayRubricRepository.deleteByQuestionExamExamId(examId);
        examViolationSettingRepository.deleteByExamExamId(examId);
        questionRepository.deleteByExamExamId(examId);
        assignmentRepository.deleteByExamExamId(examId);
    }

    private void saveAssignments(
            Exam exam,
            List<String> classOfferingIds,
            String assignedBy,
            ExamStatus status
    ) {
        if (classOfferingIds == null || classOfferingIds.isEmpty()) {
            return;
        }

        for (String classOfferingId : classOfferingIds) {
            if (classOfferingId == null || classOfferingId.isBlank()) {
                continue;
            }

            ExamAssignment assignment = new ExamAssignment();
            assignment.setExam(exam);
            assignment.setClassOfferingId(classOfferingId);
            assignment.setAssignedBy(assignedBy);
            assignment.setStartTime(exam.getStartDateTime());
            assignment.setEndTime(exam.getEndDateTime());
            assignment.setStatus(status);

            assignmentRepository.save(assignment);
        }
    }

    private void saveQuestions(Exam exam, List<QuestionRequest> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        List<ExamQuestion> questionEntities = new ArrayList<>();

        int order = 1;

        for (QuestionRequest q : questions) {
            String rawType = q.getQuestionType();

            QuestionType questionType;

            try {
                questionType = QuestionType.valueOf(
                        rawType.trim().toUpperCase()
                );
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid question type: " + rawType
                );
            }

            ExamQuestion question = new ExamQuestion();

            question.setExam(exam);
            question.setQuestionType(questionType);
            question.setQuestionText(q.getQuestionText());
            question.setQuestionImageUrl(q.getQuestionImageUrl());
            question.setPoints( q.getPoints() == null ? BigDecimal.ONE : q.getPoints() );
            question.setCorrectAnswer(q.getCorrectAnswer());
            question.setQuestionOrder(q.getQuestionOrder() == null ? order : q.getQuestionOrder());
            question.setQuestionInstruction(q.getQuestionInstruction());
            questionEntities.add(question);

            order++;
        }

        List<ExamQuestion> savedQuestions = questionRepository.saveAll(questionEntities);
        List<ExamChoice> choiceEntities = getExamChoices(questions, savedQuestions);
        if (!choiceEntities.isEmpty()) { choiceRepository.saveAll(choiceEntities); }

        List<EssayRubric> rubricEntities = getEssayRubrics(questions, savedQuestions);
        if (!rubricEntities.isEmpty()) { essayRubricRepository.saveAll(rubricEntities); }


    }

    private static @NonNull List<ExamChoice> getExamChoices(List<QuestionRequest> questions, List<ExamQuestion> savedQuestions) {
        List<ExamChoice> choiceEntities = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            QuestionRequest q = questions.get(i);
            ExamQuestion savedQuestion = savedQuestions.get(i);

            String questionType = q.getQuestionType().trim().toUpperCase();

            if (!"MULTIPLE_CHOICE".equals(questionType)) { continue; }

            List<ChoiceRequest> choices = q.getChoices();

            if (choices == null || choices.isEmpty()) { continue; }

            int choiceOrder = 1;

            for (ChoiceRequest c : choices) {
                ExamChoice choice = new ExamChoice();
                choice.setQuestion(savedQuestion);
                choice.setChoiceLabel(c.getChoiceLabel());
                choice.setChoiceText(c.getChoiceText());
                choice.setChoiceImageUrl(c.getChoiceImageUrl());
                choice.setCorrect(Boolean.TRUE.equals(c.getCorrect()));
                choice.setChoiceOrder(c.getChoiceOrder() == null ? choiceOrder : c.getChoiceOrder());

                choiceEntities.add(choice);
                choiceOrder++;
            }
        }
        return choiceEntities;
    }

    private List<EssayRubric> getEssayRubrics(
            List<QuestionRequest> questions,
            List<ExamQuestion> savedQuestions
    ) {
        List<EssayRubric> rubricEntities = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            QuestionRequest q = questions.get(i);
            ExamQuestion savedQuestion = savedQuestions.get(i);

            if (!"ESSAY".equalsIgnoreCase(q.getQuestionType())) { continue; }

            if (q.getRubrics() == null || q.getRubrics().isEmpty()) { continue; }

            int order = 1;

            for (EssayRubricRequest r : q.getRubrics()) {
                EssayRubric rubric = new EssayRubric();

                rubric.setQuestion(savedQuestion);
                rubric.setCriterionName(r.getCriterionName());
                rubric.setWeightPercentage(r.getWeightPercentage());
                rubric.setDescription(r.getDescription());
                rubric.setDisplayOrder(r.getDisplayOrder() == null ? order : r.getDisplayOrder());

                rubricEntities.add(rubric);
                order++;
            }
        }

        return rubricEntities;
    }

    @Transactional
    public SimpleMessageResponse applyViolationDecision(
            ViolationDecisionRequest request,
            String employeeId,
            String role
    ) {
        validateRole(role);

        ExamAnswer answer = answerRepository.findById(request.getAnswerId())
                .orElseThrow(() -> new RuntimeException("Answer not found."));

        BigDecimal currentScore = answer.getPointsAwarded() == null
                ? BigDecimal.ZERO
                : answer.getPointsAwarded();

        BigDecimal deduction = request.getDeduction() == null
                ? BigDecimal.ZERO
                : request.getDeduction();

        if (deduction.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Deduction cannot be negative.");
        }

        if (deduction.compareTo(currentScore) > 0) {
            throw new RuntimeException("Deduction cannot be greater than current score.");
        }

        String decision = request.getDecision() == null
                ? "IGNORED"
                : request.getDecision().trim().toUpperCase();

        if (!decision.equals("IGNORED") && !decision.equals("PENALIZED")) {
            throw new RuntimeException("Invalid violation decision.");
        }

        if (decision.equals("PENALIZED")) {
            answer.setPointsAwarded(currentScore.subtract(deduction));
        }

        String existingFeedback = answer.getFacultyFeedback();

        String newFeedbackBlock =
                "\n\n[Violation Decision]\n" +
                        "Decision: " + decision + "\n" +
                        "Deduction: " + deduction + "\n" +
                        "Notes: " + safeText(request.getFeedback());

        answer.setFacultyFeedback(
                existingFeedback == null || existingFeedback.isBlank()
                        ? newFeedbackBlock.trim()
                        : existingFeedback.trim() + newFeedbackBlock
        );

        answer.setManuallyReviewed(true);
        answer.setNeedsChecking(false);
        answer.setReviewStatus("REVIEWED");

        answerRepository.save(answer);

        violationLogRepository.markQuestionViolationsReviewed(
                request.getAttemptId(),
                request.getQuestionId(),
                decision,
                employeeId,
                OffsetDateTime.now()
        );

        return new SimpleMessageResponse(true, "Violation decision saved.");
    }

    private String safeText(String value) {
        return value == null || value.isBlank()
                ? "No notes provided."
                : value.trim();
    }


    // ===============
    // HELPERS
    // ===============

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String determineStatus(Exam exam, List<ExamAssignment> assignments) {
        return examStatusService.getDisplayStatus(exam).name();
    }

    private String formatAssignedTo(
            List<ExamAssignment> assignments,
            Map<String, String> classOfferingDisplayMap
    ) {
        if (assignments == null || assignments.isEmpty()) {
            return "Not assigned";
        }

        return assignments.stream()
                .map(a -> classOfferingDisplayMap.getOrDefault(a.getClassOfferingId(), "Unknown"))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String getValidUntil(List<ExamAssignment> assignments) {

        if (assignments == null || assignments.isEmpty()) {
            return "-";
        }

        return assignments.stream()
                .filter(a -> a.getEndTime() != null)
                .map(a -> a.getEndTime().format(DISPLAY_DATE))
                .findFirst()
                .orElse("-");
    }

    private boolean isIdentificationAnswerCorrect(String studentAnswer, String correctAnswer) {
        if (isBlank(studentAnswer) || isBlank(correctAnswer)) {
            return false;
        }

        String normalizedStudentAnswer = normalizeAnswer(studentAnswer);

        String[] acceptedAnswers = correctAnswer.split(";");

        for (String acceptedAnswer : acceptedAnswers) {
            if (normalizeAnswer(acceptedAnswer).equals(normalizedStudentAnswer)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeAnswer(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private String formatDuration(Integer minutes) {

        if (minutes == null || minutes <= 0) {
            return "No limit";
        }

        if (minutes < 60) {
            return minutes + " mins";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) {
            return hours == 1 ? "1 hr" : hours + " hrs";
        }

        return hours + " hr " + remainingMinutes + " mins";
    }

    private String formatTakers(int taken, Integer assignedCount) {

        if (assignedCount == null || assignedCount <= 0) {
            return "0% (0/0)";
        }

        int percent = (int) Math.round((taken * 100.0) / assignedCount);

        return percent + "% (" + taken + "/" + assignedCount + ")";
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(DISPLAY_DATE);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        return dateTime
                .atZoneSameInstant(ZoneId.of("Asia/Manila"))
                .toLocalDateTime()
                .format(DISPLAY_DATE_TIME);
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

}