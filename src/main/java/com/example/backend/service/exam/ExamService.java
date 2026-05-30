package com.example.backend.service.exam;

import com.example.backend.audit.ActivityTarget;
import com.example.backend.audit.ActivityContext;
import com.example.backend.audit.ActivityTargetType;
import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.exam.request.*;
import com.example.backend.dto.exam.response.*;
import com.example.backend.dto.exam.result.ExamResult;
import com.example.backend.dto.exam.result.ExamTakingRawContent;
import com.example.backend.dto.faculty.request.ViolationDecisionRequest;
import com.example.backend.dto.faculty.response.SimpleMessageResponse;
import com.example.backend.entity.cache.ClassOfferingCache;
import com.example.backend.entity.cache.FacultyProfileCache;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.AdminProfile;
import com.example.backend.entity.enums.ExamAttemptStatus;
import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.enums.QuestionType;
import com.example.backend.entity.exam.*;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.exam.*;
import com.example.backend.repository.cache.*;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.entity.enums.ExamStatus;
import com.example.backend.service.core.EmailService;
import com.example.backend.service.student.StudentEvictCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
@RequiredArgsConstructor
public class ExamService {

    // Database Repositories
    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamChoiceRepository choiceRepository;
    private final ExamAssignmentRepository assignmentRepository;
    private final ExamViolationSettingRepository examViolationSettingRepository;
    private final QuestionViolationOverrideRepository questionViolationOverrideRepository;
    private final ExamStatusService examStatusService;
    private final ClassOfferingCacheRepository classOfferingCacheRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final FacultyLoadCacheRepository facultyLoadCacheRepository;
    private final ExamTemplateService examTemplateService;
    private final EmailService emailService;
    private final ExamAttemptRepository attemptRepository;
    private final ExamAttemptChoiceOrderRepository attemptChoiceOrderRepository;
    private final ExamReadCacheService examReadCacheService;
    private final ExamViolationLogRepository violationLogRepository;
    private final ExamAnswerRepository answerRepository;
    private final EssayRubricRepository essayRubricRepository;
    private final EssayRubricScoreRepository rubricScoreRepository;
    private final ExamAnswerReviewLogRepository reviewLogRepository;
    private final ClassEnrollmentCacheRepository classEnrollmentCacheRepository;
    private final ExamWorkspaceRepository examWorkspaceRepository;
    private final StudentEvictCacheService studentEvictCacheService;

    private final Gson gson = new Gson();

    // Lobby Opened
    private static final int LOBBY_OPEN_MINUTES_BEFORE_START = 15;

    // Time Converter
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");

    @Value("${exam.upload.dir}")
    private String examUploadDir;

    @Value("${evidence.upload.dir}")
    private String evidenceUploadDir;

    @TrackActivity(
            module = "EXAM",
            action = "SAVE_DRAFT",
            message = "Exam draft creation attempted"
    )
    @Transactional
    public ExamResult examDraft(
            ExamRequest request,
            String schoolId,
            String role
    ) {
        return createExam(request, schoolId, role, false);
    }

    @TrackActivity(
            module = "EXAM",
            action = "CREATE_AND_PUBLISH",
            message = "Exam creation and publish attempted"
    )
    @Transactional
    public ExamResult examPublish(
            ExamRequest request,
            String schoolId,
            String role
    ) {
        return createExam(request, schoolId, role, true);
    }

    @TrackActivity(
            module = "EXAM",
            action = "UPDATE_EXAM",
            message = "Exam update attempted"
    )
    @Transactional
    public ExamResult updateExam(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,
            ExamRequest request,
            String schoolId,
            String role
    ) {

        Exam exam = examRepository.findById(examId).orElse(null);

        if (exam == null) { return new ExamResult(false, "Exam not found.", null, 0); }
        if (exam.getStatus() == CANCELLED) { return new ExamResult(false, "Cancelled exam cannot be edited.", examId, 0); }
        if (exam.getStatus() == ExamStatus.COMPLETED) { return new ExamResult(false, "Completed exam cannot be edited.", examId, 0); }

        String validation = validateExamRequest(request, true);

        if (validation != null) {
            return new ExamResult(false, validation, examId, 0);
        }

        applyExamFields(exam, request);
        exam.setUpdatedBy(schoolId);
        exam.setUpdatedByRole(role);

        Exam savedExam = examRepository.save(exam);

        syncAssignments(savedExam, request.getClassOfferingIds(), schoolId, savedExam.getStatus());
        syncQuestions(savedExam, request.getQuestions());
        saveViolationSettings(savedExam, request.getViolationSettings());

        examReadCacheService.refreshStudentExamCachesAfterExamChange(
                request.getClassOfferingIds(),
                savedExam.getExamId()
        );

        return new ExamResult(
                true,
                "Exam updated successfully.",
                savedExam.getExamId(),
                request.getQuestions() == null ? 0 : request.getQuestions().size()
        );
    }

    @TrackActivity(
            module = "EXAM",
            action = "PUBLISH_EXAM",
            message = "Exam publish attempted"
    )
    @Transactional
    public ExamResult publishExamById(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,
            String schoolId,
            String role
    ) {

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

        List<ExamAssignment> assignments = assignmentRepository.findByExamExamId(examId);

        for (ExamAssignment assignment : assignments) {
            assignment.setStatus(PUBLISHED);
        }

        assignmentRepository.saveAll(assignments);

        List<String> classOfferingIds = assignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .toList();

        studentEvictCacheService.evictAllStudents();

        notifyStudents(savedExam, classOfferingIds, ExamStatus.PUBLISHED);

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
        log.setEvidenceUrl(request.getEvidenceUrl());
        log.setEvidenceType(request.getEvidenceType());
        log.setEvidenceSource(request.getEvidenceSource());

        try {

            if (request.getEvidenceMetadata() != null && !request.getEvidenceMetadata().isBlank()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode metadataNode = mapper.readTree(request.getEvidenceMetadata());
                log.setEvidenceMetadata(metadataNode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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

        return new ExamResult(true, "Activity logged.", request.getExamId(), 1);
    }

    @TrackActivity(
            module = "EXAM",
            action = "CANCEL_EXAM",
            message = "Exam cancellation attempted"
    )
    @Transactional
    public ExamResult cancelExam(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,
            String schoolId,
            String role
    ) {

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

        examReadCacheService.evictExamTakingRawContent(examId);

        return new ExamResult(true, "Exam cancelled successfully.", examId, 0);
    }

    @TrackActivity(
            module = "EXAM",
            action = "RESTORE_EXAM",
            message = "Exam restore attempted"
    )
    @Transactional
    public ExamResult restoreExam(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,
            String schoolId,
            String role
    ) {

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

    @TrackActivity(
            module = "EXAM_REVIEW",
            action = "SAVE_ESSAY_REVIEW",
            message = "Essay review save attempted"
    )
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

        ActivityContext.setTargetUserId(answer.getAttempt().getStudentId());
        ActivityContext.setExamId(answer.getAttempt().getExamId());
        ActivityContext.setAttemptId(answer.getAttempt().getAttemptId());
        ActivityContext.setQuestionId(answer.getQuestion().getQuestionId());

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
                    .findByAnswerAnswerIdAndRubricRubricId(answer.getAnswerId(),
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
        answer.setFacultyFeedback(request.getFacultyFeedback());
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

    @TrackActivity(
            module = "EXAM",
            action = "REPLACE_EXAM_TEMPLATE",
            message = "Exam template replacement attempted"
    )
    @Transactional
    public ExamResult replaceUploadExamTemplate(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,

            MultipartFile file
    ) {

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

    @TrackActivity(
            module = "EXAM_TAKING",
            action = "SAVE_ANSWER",
            message = "Student answer save attempted"
    )
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

        ActivityContext.setTargetUserId(studentId);
        ActivityContext.setExamId(attempt.getExamId());
        ActivityContext.setAttemptId(attempt.getAttemptId());
        ActivityContext.setQuestionId(question.getQuestionId());

        boolean hasViolation =
                violationLogRepository.existsByAttemptAttemptIdAndQuestionQuestionId(
                                attempt.getAttemptId(),
                                question.getQuestionId()
                        );

        ExamAnswer answer =
                answerRepository.findByAttemptAttemptIdAndQuestionQuestionId(
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

    @TrackActivity(
            module = "EXAM_TAKING",
            action = "SUBMIT_EXAM",
            message = "Student exam submission attempted"
    )
    @Transactional
    public ExamResult submitExam(
            @ActivityTarget(ActivityTargetType.ATTEMPT_ID)
            Long attemptId,
            String studentId
    ) {

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

        System.out.println("SUBMIT HIT IN RAILWAY: " + studentId);
        studentEvictCacheService.evictStudent(studentId);

        return new ExamResult(
                true,
                "Exam submitted successfully.",
                attempt.getExamId(),
                1
        );
    }

    @TrackActivity(
            module = "EXAM_TAKING",
            action = "BEGIN_EXAM_ATTEMPT",
            message = "Student began exam attempt"
    )
    @Transactional
    public ExamTakingResponse beginExamAttempt(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,
            String schoolId,
            String role
    ) {
        /*
         * This method is the real exam start gate.
         *
         * Important:
         * - Lobby can open early.
         * - This method blocks students until exact start time.
         * - Timer starts here by setting attempt.startedAt.
         */

        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only students can begin exams."
            );
        }

        Exam exam = examRepository
                .findById(examId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exam not found."
                ));

        if (exam.getStartDateTime() == null || exam.getEndDateTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Exam schedule is invalid."
            );
        }

        OffsetDateTime now = nowManila();

        OffsetDateTime examStart = exam.getStartDateTime();
        OffsetDateTime examEnd = exam.getEndDateTime();

        OffsetDateTime startManila = toManila(examStart);
        OffsetDateTime endManila = toManila(examEnd);

        if (now.isBefore(startManila)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot begin yet. The exam can only start at the scheduled start time."
            );
        }

        if (now.isAfter(endManila)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This exam has already ended."
            );
        }

        ExamTakingRawContent rawContent =
                examReadCacheService.getExamTakingRawContent(examId);

        Optional<ExamAttempt> existingAttempt =
                attemptRepository.findByExamIdAndStudentId(examId, schoolId);

        ExamAttempt attempt;

        if (existingAttempt.isPresent()) {
            attempt = existingAttempt.get();

            if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This exam attempt is already submitted."
                );
            }

        } else {
            attempt = createNewAttempt(
                    exam,
                    schoolId,
                    rawContent.getQuestions(),
                    rawContent.getChoiceMap()
            );
        }

        /*
         * Synchronous exam:
         * Timer is based on official exam start.
         *
         * Asynchronous exam:
         * Timer is based on when student actually clicks Begin Exam.
         */
        if (attempt.getStartedAt() == null) {
            if (exam.getExamMode() == ExamMode.SYNCHRONOUS) {
                attempt.setStartedAt(examStart);
            } else {
                attempt.setStartedAt(now);
            }

            attemptRepository.save(attempt);
        }

        int timeLimitMinutes =
                exam.getTimeLimitMinutes() == null
                        ? 60
                        : exam.getTimeLimitMinutes();

        OffsetDateTime timerStartedAt =
                exam.getExamMode() == ExamMode.SYNCHRONOUS
                        ? startManila
                        : toManila(attempt.getStartedAt());

        long elapsedSeconds =
                java.time.Duration.between(timerStartedAt, now).getSeconds();

        long remainingSeconds =
                Math.max(0, (timeLimitMinutes * 60L) - elapsedSeconds);

        if (remainingSeconds <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your exam time has already ended."
            );
        }

        ExamTakingResponse response =
                getExamForTaking(examId, schoolId, role);

        response.setAttemptStartedAt(attempt.getStartedAt());
        response.setAttemptId(attempt.getAttemptId());
        response.setServerNow(now);
        response.setCanBeginExam(true);
        response.setRemainingSeconds(remainingSeconds);

        return response;
    }

    @TrackActivity(
            module = "EXAM_REVIEW",
            action = "APPLY_VIOLATION_DECISION",
            message = "Violation decision attempted"
    )
    @Transactional
    public SimpleMessageResponse applyViolationDecision(
            ViolationDecisionRequest request,
            String employeeId,
            String role
    ) {
        validateRole(role);

        ExamAnswer answer = answerRepository.findById(request.getAnswerId())
                .orElseThrow(() -> new RuntimeException("Answer not found."));

        ActivityContext.setTargetUserId(answer.getAttempt().getStudentId());
        ActivityContext.setExamId(answer.getAttempt().getExamId());
        ActivityContext.setAttemptId(answer.getAttempt().getAttemptId());
        ActivityContext.setQuestionId(answer.getQuestion().getQuestionId());

        BigDecimal deduction =
                request.getDeduction() == null ? BigDecimal.ZERO : request.getDeduction();

        if (deduction.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Deduction cannot be negative.");
        }

        String decision = request.getDecision() == null
                ? "IGNORED"
                : request.getDecision().trim().toUpperCase();

        if (!decision.equals("IGNORED") && !decision.equals("PENALIZED")) {
            throw new RuntimeException("Invalid violation decision.");
        }

        BigDecimal scoreBefore = answer.getPointsAwarded() == null
                ? BigDecimal.ZERO
                : answer.getPointsAwarded();

        BigDecimal scoreAfter = scoreBefore;

        if ("PENALIZED".equalsIgnoreCase(decision)) {
            if (deduction.compareTo(scoreBefore) > 0) {
                throw new RuntimeException("Deduction cannot be greater than current score.");
            }

            scoreAfter = scoreBefore.subtract(deduction);
        }

        List<ExamViolationLog> violations =
                violationLogRepository
                        .findByAttemptAttemptIdAndQuestionQuestionId(
                                request.getAttemptId(),
                                request.getQuestionId()
                        );

        OffsetDateTime reviewedAt = OffsetDateTime.now();

        answer.setPointsAwarded(scoreAfter);
        answer.setManuallyReviewed(true);
        answer.setNeedsChecking(false);
        answer.setReviewStatus("REVIEWED");
        answerRepository.save(answer);

        for (ExamViolationLog violation : violations) {
            String previousStatus = violation.getReviewStatus();

            violation.setReviewStatus(decision);
            violation.setReviewedBy(employeeId);
            violation.setReviewedAt(reviewedAt);
            violation.setReviewNotes(request.getFeedback());
            violationLogRepository.save(violation);

            ExamAnswerReviewLog log = new ExamAnswerReviewLog();
            log.setExam(violation.getExam());
            log.setAttempt(violation.getAttempt());
            log.setAnswer(answer);
            log.setQuestion(violation.getQuestion());
            log.setViolation(violation);
            log.setActionType(
                    "PENALIZED".equalsIgnoreCase(decision)
                            ? "VIOLATION_PENALIZED"
                            : "VIOLATION_IGNORED"
            );
            log.setPreviousValue(previousStatus);
            log.setNewValue(decision);
            log.setScoreBefore(scoreBefore);
            log.setScoreAfter(scoreAfter);
            log.setDeduction(deduction);
            log.setNotes(request.getFeedback());
            log.setCreatedBy(employeeId);
            log.setCreatedByRole(role);

            reviewLogRepository.save(log);
        }

        recomputeAttemptScore(answer.getAttempt());

        return new SimpleMessageResponse(true, "Violation decision saved.");
    }

    private ExamAttempt createNewAttempt(
            Exam exam,
            String username,
            List<ExamQuestion> dbQuestions,
            Map<Long, List<ExamChoice>> choiceMap
    ) {
        /*
         * This creates an attempt shell.
         *
         * It is allowed during lobby time.
         * It saves stable question and choice order.
         * It must NOT start the timer.
         */

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

        /*
         * Do not set startedAt here.
         * If we set it during lobby, the timer will start too early.
         */
        attempt.setStartedAt(null);

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

    private void saveViolationSettings(
            Exam exam,
            List<ViolationSettingRequest> requests
    ) {

        examViolationSettingRepository.deleteByExamIdNow(exam.getExamId());
        examViolationSettingRepository.flush();

        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<ExamViolationSetting> settings = requests.stream()
                .map(request -> {

                    ExamViolationSetting setting =
                            new ExamViolationSetting();

                    setting.setExam(exam);
                    setting.setViolationType(request.getViolationType());

                    setting.setEnabled(request.getEnabled());

                    setting.setSeverity(
                            request.getSeverity()
                    );

                    setting.setMaxAllowedCount(
                            request.getMaxAllowedCount()
                    );

                    return setting;
                })
                .toList();

        examViolationSettingRepository.saveAll(settings);
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
            examReadCacheService.warmExamTakingRawContent(exam.getExamId());
            studentEvictCacheService.evictAllStudents();
        }

        return new ExamResult(
                true,
                publishNow ? "Exam published successfully." : "Exam saved as draft.",
                savedExam.getExamId(),
                request.getQuestions() == null ? 0 : request.getQuestions().size()
        );
    }

    /**
     *
     * @param role
     * @param schoolId
     * @return
     */
    public List<ExamResponse> getAllExams(String role, String schoolId) {

        long start = System.currentTimeMillis();

        List<Exam> exams;

        // ADMIN GETS ALL EXAM
        if ("ADMIN".equalsIgnoreCase(role)) { exams = examRepository.findAll(); }

        // FACULTY GET EXAMS ASSIGNED TO HIS/HER
        else if ("FACULTY".equalsIgnoreCase(role)) { exams = examRepository.findVisibleExamsForFaculty(schoolId);
        }

        // EMPTY LIST
        else { return List.of(); }


        if (exams.isEmpty()) { return List.of(); }

        // Get the examIds
        List<Long> examIds = exams.stream().map(Exam::getExamId).toList();

        // Get all assigned exams based on examIds
        List<ExamAssignment> allAssignments = assignmentRepository.findByExamExamIdIn(examIds);
        Map<Long, List<ExamAssignment>> assignmentMap = allAssignments.stream() .collect(Collectors.groupingBy(
                a -> a.getExam().getExamId()));

        // Count the student takers
        List<Object[]> takerRows = examRepository.countTakersByExamIds(examIds);
        Map<Long, Integer> takerMap = new HashMap<>();

        for (Object[] row : takerRows) {
            Long rowExamId = (Long) row[0];
            Number count = (Number) row[1];
            takerMap.put(rowExamId, count.intValue());
        }

        // Count the students submitted exams
        List<Object[]> submittedRows = examRepository.countSubmittedTakersByExamIds(examIds);
        Map<Long, Integer> submittedMap = new HashMap<>();

        for (Object[] row : submittedRows) {
            Long rowExamId = (Long) row[0];
            Number count = (Number) row[1];
            submittedMap.put(rowExamId, count.intValue());
        }

        /* Format the display name (i.e: Juan Dela Cruz(Faculty)) */
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

        Map<String, ClassOfferingMeta> classOfferingMetaMap =
                buildClassOfferingMetaMap(classOfferingIds);

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
                            formatAssignedTo(assignments, classOfferingMetaMap),
                            getTerm(assignments, classOfferingMetaMap),
                            getAcademicYear(assignments, classOfferingMetaMap),
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

    public ExamResponse viewExam(Long examId, String userId, String role) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        List<ExamAssignment> assignments =
                assignmentRepository.findByExamExamId(examId);

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isFaculty = "FACULTY".equalsIgnoreCase(role);

        if (isFaculty) {
            boolean createdByFaculty = Objects.equals(exam.getCreatedBy(), userId);

            boolean assignedToFaculty = assignments.stream()
                    .map(ExamAssignment::getClassOfferingId)
                    .filter(Objects::nonNull)
                    .anyMatch(classOfferingId ->
                            facultyLoadCacheRepository.existsByEmployeeIdAndClassOfferingId(
                                    userId,
                                    classOfferingId
                            )
                    );

            if (!createdByFaculty && !assignedToFaculty) {
                throw new RuntimeException("You are not authorized to view this exam.");
            }
        }

        if (!isAdmin && !isFaculty) {
            throw new RuntimeException("You are not authorized to view this exam.");
        }

        Set<String> classOfferingIds = assignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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

        Map<String, ClassOfferingMeta> classOfferingMetaMap = buildClassOfferingMetaMap(classOfferingIds);

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
                formatAssignedTo(assignments, classOfferingMetaMap),
                getTerm(assignments, classOfferingMetaMap),
                getAcademicYear(assignments, classOfferingMetaMap),
                formatTakers(submittedTakers, totalTakers),
                formatDateTime(exam.getStartDateTime()),
                formatDateTime(exam.getEndDateTime()),
                exam.getExamMode() == null ? "" : exam.getExamMode().name(),
                userDisplayMap.getOrDefault(exam.getCreatedBy(), exam.getCreatedBy()),
                userDisplayMap.getOrDefault(exam.getUpdatedBy(), exam.getUpdatedBy()),
                questionPreviews
        );

        response.setClassOfferingIds(new ArrayList<>(classOfferingIds));
        response.setTimeLimitMinutes(exam.getTimeLimitMinutes());
        response.setShuffleQuestions(exam.getShuffleQuestions());
        response.setShuffleChoices(exam.getShuffleChoices());
        response.setRawStartDateTime(exam.getStartDateTime());
        response.setRawEndDateTime(exam.getEndDateTime());

        return response;
    }

    @TrackActivity(
            module = "RESULTS",
            action = "RELEASE_RESULTS",
            message = "Exam results release attempted"
    )
    @Transactional
    public SimpleMessageResponse releaseExamResults(
            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,
            String employeeId,
            String role
    ) {
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

                return new SimpleMessageResponse(
                        false,
                        "Results are already released."
                );
            }

            backfillAttemptScoresBeforeRelease(examId);
            exam.setResultsReleased(true);
            exam.setResultsReleasedAt(OffsetDateTime.now());
            Exam savedExam = examRepository.save(exam);

            List<ExamAssignment> assignments = assignmentRepository.findByExamExamId(examId);

            List<String> classOfferingIds = assignments.stream()
                    .map(ExamAssignment::getClassOfferingId)
                    .toList();

            notifyStudents(savedExam, classOfferingIds, ExamStatus.RESULTS_RELEASED);

            studentEvictCacheService.evictAllStudents();

            return new SimpleMessageResponse(
                    true,
                    "Results released successfully."
            );

        } catch (Exception e) {

            throw e;
        }
    }

    private void backfillAttemptScoresBeforeRelease(Long examId) {

        List<ExamAttempt> attempts =
                attemptRepository.findByExamId(examId);

        for (ExamAttempt attempt : attempts) {

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
                    answerRepository.existsPendingReviewByAttemptId(
                            attempt.getAttemptId()
                    );

            if (!needsReview
                    && !"REVIEWED".equalsIgnoreCase(attempt.getReviewStatus())) {
                attempt.setReviewStatus("REVIEWED");
                attempt.setReviewedBy("SYSTEM");
                attempt.setReviewedAt(OffsetDateTime.now());
            }

            attemptRepository.save(attempt);
        }
    }


    private Map<String, ClassOfferingMeta> buildClassOfferingMetaMap(Set<String> classOfferingIds) {

        if (classOfferingIds == null || classOfferingIds.isEmpty()) {
            return Map.of();
        }

        return classOfferingCacheRepository
                .findByClassOfferingIdIn(new ArrayList<>(classOfferingIds))
                .stream()
                .collect(Collectors.toMap(
                        co -> co.getClassOfferingId(),
                        co -> new ClassOfferingMeta(
                                co.getProgramCode(),
                                co.getTerm(),
                                co.getAcademicYear()
                        ),
                        (a, b) -> a
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

    public List<ClassOfferingResponse> getClassOfferings(String schoolId, String role) {

        List<ClassOfferingCache> offerings;

        if ("ADMIN".equalsIgnoreCase(role)) {
            offerings = classOfferingCacheRepository.findAllActive();

        } else if ("FACULTY".equalsIgnoreCase(role)) {
            offerings = facultyLoadCacheRepository.findActiveOfferingsByEmployeeId(schoolId);
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
            return new ImageUploadResponse(
                    false,
                    "Image file is empty.",
                    null
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            return new ImageUploadResponse(
                    false,
                    "Only image files are allowed.",
                    null
            );
        }

        try {

            String uploadDir = examUploadDir;

            File directory = new File(uploadDir);

            if (!directory.exists()) {
                boolean created = directory.mkdirs();

                if (!created) {
                    return new ImageUploadResponse(
                            false,
                            "Failed to create upload directory.",
                            null
                    );
                }
            }

            String filename = UUID.randomUUID() + ".jpg";

            File destination = new File(directory, filename);

            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            double quality = 0.9;

            boolean success = false;

            while (quality >= 0.1) {

                Thumbnails.of(originalImage)
                        .size(1200, 1200)
                        .outputFormat("jpg")
                        .outputQuality(quality)
                        .toFile(destination);

                long fileSize = destination.length();

                if (fileSize <= 1_000_000) {
                    success = true;
                    break;
                }

                quality -= 0.1;
            }

            if (!success) {
                return new ImageUploadResponse(
                        false,
                        "Unable to compress image below 1MB.",
                        null
                );
            }

            String imageUrl = "/uploads/exams/" + filename;

            return new ImageUploadResponse(
                    true,
                    "Image uploaded successfully.",
                    imageUrl
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new ImageUploadResponse(
                    false,
                    "Failed to save image.",
                    null
            );
        }
    }

    public ImageUploadResponse uploadViolationEvidence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ImageUploadResponse(false, "Evidence file is empty.", null);
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            return new ImageUploadResponse(false, "Only image files are allowed.", null);
        }

        try {
            String uploadDir = evidenceUploadDir;

            File directory = new File(uploadDir);

            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    return new ImageUploadResponse(false, "Failed to create evidence directory.", null);
                }
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = "evidence-" + UUID.randomUUID() + extension;

            File destination = new File(directory, filename);

            Thumbnails.of(file.getInputStream())
                    .size(1200, 1200)
                    .outputQuality(0.85)
                    .toFile(destination);

            String imageUrl = "/uploads/evidence/" + filename;

            return new ImageUploadResponse(true, "Evidence uploaded successfully.", imageUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return new ImageUploadResponse(false, "Failed to save evidence.", null);
        }
    }

    public ExamTakingResponse getExamForTaking(
            Long examId,
            String schoolId,
            String role
    ) {
        /*
         * This method prepares the lobby/taking data.
         *
         * Important:
         * - Student may enter lobby 15 minutes before start.
         * - Student must NOT start answering here.
         * - Timer starts only in beginExamAttempt().
         */

        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only students can take exams."
            );
        }

        ExamTakingRawContent rawContent =
                examReadCacheService.getExamTakingRawContent(examId);

        if (rawContent == null || rawContent.getExam() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Exam not found."
            );
        }

        Exam exam = rawContent.getExam();

        if (exam.getStartDateTime() == null || exam.getEndDateTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Exam schedule is invalid."
            );
        }

        OffsetDateTime now = nowManila();
        OffsetDateTime examStart = exam.getStartDateTime();
        OffsetDateTime examEnd = exam.getEndDateTime();

        OffsetDateTime startManila = toManila(examStart);
        OffsetDateTime endManila = toManila(examEnd);
        OffsetDateTime lobbyOpenAt = startManila.minusMinutes(LOBBY_OPEN_MINUTES_BEFORE_START);

        if (now.isBefore(lobbyOpenAt)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Lobby opens 15 minutes before the exam start time."
            );
        }

        if (now.isAfter(endManila)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This exam has already ended."
            );
        }

        boolean canBeginExam =
                !now.isBefore(startManila)
                        && !now.isAfter(endManila);

        List<ExamQuestion> dbQuestions = rawContent.getQuestions();
        Map<Long, List<ExamChoice>> choiceMap = rawContent.getChoiceMap();

        Optional<ExamAttempt> existingAttempt =
                attemptRepository.findByExamIdAndStudentId(examId, schoolId);

        ExamAttempt attempt;

        if (existingAttempt.isPresent()) {
            attempt = existingAttempt.get();

            if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This exam attempt is already submitted."
                );
            }

        } else {
            /*
             * This creates the attempt shell only.
             * It stores question/choice order for consistency.
             * It does NOT start the timer.
             */
            attempt = createNewAttempt(
                    exam,
                    schoolId,
                    dbQuestions,
                    choiceMap
            );
        }

        int timeLimitMinutes =
                exam.getTimeLimitMinutes() == null
                        ? 60
                        : exam.getTimeLimitMinutes();

        long remainingSeconds = timeLimitMinutes * 60L;

        /*
         * Timer only counts after startedAt is set.
         * startedAt is set only by beginExamAttempt().
         */
        if (attempt.getStartedAt() != null) {
            OffsetDateTime timerStartedAt;

            if (exam.getExamMode() == ExamMode.SYNCHRONOUS) {
                timerStartedAt = startManila;
            } else {
                timerStartedAt = toManila(attempt.getStartedAt());
            }

            long elapsedSeconds =
                    java.time.Duration.between(timerStartedAt, now).getSeconds();

            remainingSeconds =
                    Math.max(0, (timeLimitMinutes * 60L) - elapsedSeconds);
        }

        List<ExamQuestion> finalQuestions;

        if (attempt.getQuestionOrder() != null) {
            List<Long> savedQuestionOrder = fromJsonList(attempt.getQuestionOrder());
            finalQuestions = applyQuestionOrder(dbQuestions, savedQuestionOrder);
        } else {
            finalQuestions = dbQuestions;
        }

        Map<Long, List<Long>> savedChoiceOrderMap =
                attemptChoiceOrderRepository.findByAttemptId(attempt.getAttemptId())
                        .stream()
                        .collect(Collectors.toMap(
                                ExamAttemptChoiceOrder::getQuestionId,
                                item -> fromJsonList(item.getChoiceOrder())
                        ));

        Map<Long, ExamAnswer> savedAnswerMap =
                answerRepository.findByAttemptAttemptId(attempt.getAttemptId())
                        .stream()
                        .collect(Collectors.toMap(
                                answer -> answer.getQuestion().getQuestionId(),
                                answer -> answer,
                                (existing, duplicate) -> existing
                        ));

        List<Long> questionIds = dbQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .toList();

        List<EssayRubric> allRubrics = questionIds.isEmpty()
                ? List.of()
                : essayRubricRepository.findByQuestionQuestionIdInOrderByQuestionQuestionIdAscDisplayOrderAsc(questionIds);

        Map<Long, List<EssayRubric>> rubricMap =
                allRubrics.stream()
                        .collect(Collectors.groupingBy(r -> r.getQuestion().getQuestionId()));

        List<ExamTakingQuestionResponse> questionResponses =
                finalQuestions.stream()
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

                            ExamAnswer savedAnswer =
                                    savedAnswerMap.get(question.getQuestionId());

                            Long savedSelectedChoiceId =
                                    savedAnswer != null && savedAnswer.getSelectedChoiceId() != null
                                            ? savedAnswer.getSelectedChoiceId().getChoiceId()
                                            : null;

                            String savedStudentAnswer =
                                    savedSelectedChoiceId != null
                                            ? String.valueOf(savedSelectedChoiceId)
                                            : savedAnswer == null
                                              ? null
                                              : savedAnswer.getAnswerText();

                            ExamTakingQuestionResponse dto = new ExamTakingQuestionResponse(
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

                            dto.setSelectedChoiceId(savedSelectedChoiceId);
                            dto.setStudentAnswer(savedStudentAnswer);

                            return dto;
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

        ExamTakingResponse response = new ExamTakingResponse(
                attempt.getAttemptId(),
                exam.getExamId(),
                exam.getTitle(),
                exam.getDescription(),
                timeLimitMinutes,
                examStart,
                examEnd,
                exam.getExamMode(),
                questionResponses,
                violationSettings
        );

        response.setServerNow(now);
        response.setLobbyOpenAt(lobbyOpenAt);
        response.setAttemptStartedAt(attempt.getStartedAt());
        response.setCanBeginExam(canBeginExam);
        response.setRemainingSeconds(remainingSeconds);

        return response;
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

    // =====================
    // QUESTION VALIDATION
    // =====================

    private String validateExamRequest(ExamRequest request, boolean editMode) {

        OffsetDateTime minimumStart = OffsetDateTime.now().plusHours(1);

        if (request == null) { return "Invalid exam request."; }
        if (isBlank(request.getTitle())) { return "Exam title is required.";        }
        if (request.getTimeLimitMinutes() != null && request.getTimeLimitMinutes() <= 0) { return "Time limit must be greater than zero."; }
        if (request.getStartDateTime() == null) { throw new RuntimeException("Start date and time is required."); }
        if (request.getEndDateTime() == null) { throw new RuntimeException("End date and time is required."); }
        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) { throw new RuntimeException("End date and time must be after start date and time."); }
        if (!editMode && request.getStartDateTime().isBefore(minimumStart)) { return "Exam start time must be at least 1 hour from now."; }
        if (request.getExamMode() == null || request.getExamMode().isBlank()) { throw new RuntimeException("Exam mode is required."); }
        if (request.getQuestions() == null || request.getQuestions().isEmpty()) { return "At least one question is required.";  }

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

    // =====================
    // EMAIL NOTIFICATION
    // =====================

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

    // =====================
    // HELPERS
    // =====================



    private record ClassOfferingMeta(
            String displayName,
            String term,
            String academicYear
    ) {}

    private String getTerm(
            List<ExamAssignment> assignments,
            Map<String, ClassOfferingMeta> metaMap
    ) {
        return assignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .filter(Objects::nonNull)
                .map(metaMap::get)
                .filter(Objects::nonNull)
                .map(ClassOfferingMeta::term)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("");
    }

    private String getAcademicYear(
            List<ExamAssignment> assignments,
            Map<String, ClassOfferingMeta> metaMap
    ) {
        return assignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .filter(Objects::nonNull)
                .map(metaMap::get)
                .filter(Objects::nonNull)
                .map(ClassOfferingMeta::academicYear)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("");
    }

    private void validateReviewerRole(String role) {
        if (!"FACULTY".equalsIgnoreCase(role)
                && !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only faculty or admin can review answers.");
        }
    }

    private OffsetDateTime nowManila() {
        return OffsetDateTime.now(MANILA_ZONE);
    }
    private OffsetDateTime toManila(OffsetDateTime value) {
        if (value == null) {
            return null;
        }

        return value
                .atZoneSameInstant(MANILA_ZONE)
                .toOffsetDateTime();
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

    private void syncAssignments(
            Exam exam,
            List<String> newClassOfferingIds,
            String assignedBy,
            ExamStatus status
    ) {
        List<ExamAssignment> existingAssignments =
                assignmentRepository.findByExamExamId(exam.getExamId());

        Set<String> existingIds = existingAssignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> newIds = newClassOfferingIds == null
                ? Set.of()
                : new HashSet<>(newClassOfferingIds);

        for (ExamAssignment existing : existingAssignments) {
            if (!newIds.contains(existing.getClassOfferingId())) {
                assignmentRepository.delete(existing);
            }
        }

        List<ExamAssignment> toAdd = new ArrayList<>();

        for (String classOfferingId : newIds) {
            if (!existingIds.contains(classOfferingId)) {
                ExamAssignment assignment = new ExamAssignment();
                assignment.setExam(exam);
                assignment.setClassOfferingId(classOfferingId);
                assignment.setAssignedBy(assignedBy);
                assignment.setStatus(status);

                toAdd.add(assignment);
            }
        }

        if (!toAdd.isEmpty()) {
            assignmentRepository.saveAll(toAdd);
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

    @Transactional
    private void syncQuestions(
            Exam exam,
            List<QuestionRequest> requests
    ) {

        List<ExamQuestion> existingQuestions =
                questionRepository.findByExamExamIdOrderByQuestionOrderAsc(exam.getExamId());

        Map<Long, ExamQuestion> existingMap =
                existingQuestions.stream().collect(Collectors.toMap(ExamQuestion::getQuestionId, q -> q));

        Set<Long> incomingIds = requests.stream()
                .map(QuestionRequest::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // DELETE REMOVED QUESTIONS
        for (ExamQuestion existing : existingQuestions) {

            if (!incomingIds.contains(existing.getQuestionId())) {

                boolean hasAttempts =
                        answerRepository.existsByQuestionQuestionId( existing.getQuestionId() );

                if (hasAttempts) {
                    throw new RuntimeException(
                            "Cannot delete question with student attempts."
                    );
                }

                deleteExamImageIfExists(existing.getQuestionImageUrl());

                questionRepository.delete(existing);
            }
        }

        int order = 1;

        for (QuestionRequest request : requests) {

            ExamQuestion question;

            // UPDATE EXISTING
            if (request.getQuestionId() != null
                    && existingMap.containsKey(request.getQuestionId())) {

                question = existingMap.get(request.getQuestionId());

            }

            // CREATE NEW
            else {

                question = new ExamQuestion();
                question.setExam(exam);
            }

            question.setQuestionType(
                    QuestionType.valueOf(request.getQuestionType())
            );

            question.setQuestionText(request.getQuestionText());

            String oldImageUrl = question.getQuestionImageUrl();
            String newImageUrl = request.getQuestionImageUrl();

            boolean imageChanged =
                    oldImageUrl != null && !oldImageUrl.isBlank() && !Objects.equals(oldImageUrl, newImageUrl);

            if (imageChanged) {
                deleteExamImageIfExists(oldImageUrl);
            }

            question.setQuestionImageUrl(newImageUrl);
            question.setPoints(request.getPoints());
            question.setCorrectAnswer(request.getCorrectAnswer());
            question.setQuestionInstruction(request.getQuestionInstruction());
            question.setQuestionOrder(order++);

            question = questionRepository.save(question);

            syncChoices(question, request.getChoices());

            syncRubrics(question, request.getRubrics());
        }
    }

    private void syncChoices(
            ExamQuestion question,
            List<ChoiceRequest> requests
    ) {

        if (requests == null) {
            requests = List.of();
        }

        List<ExamChoice> existingChoices =
                choiceRepository.findByQuestionQuestionIdOrderByChoiceOrderAsc(
                        question.getQuestionId()
                );

        Map<Long, ExamChoice> existingMap =
                existingChoices
                        .stream()
                        .collect(Collectors.toMap(
                                ExamChoice::getChoiceId,
                                c -> c
                        ));

        Set<Long> incomingIds =
                requests
                        .stream()
                        .map(ChoiceRequest::getChoiceId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        for (ExamChoice existing : existingChoices) {

            if (!incomingIds.contains(existing.getChoiceId())) {

                deleteExamImageIfExists(existing.getChoiceImageUrl());

                choiceRepository.delete(existing);
            }
        }

        int order = 1;

        for (ChoiceRequest request : requests) {

            ExamChoice choice;

            if (request.getChoiceId() != null && existingMap.containsKey(request.getChoiceId())) {

                choice = existingMap.get(request.getChoiceId());

            } else {

                choice = new ExamChoice();
                choice.setQuestion(question);
            }

            String oldImageUrl = choice.getChoiceImageUrl();
            String newImageUrl = request.getChoiceImageUrl();

            boolean imageChanged =
                    oldImageUrl != null && !oldImageUrl.isBlank() && !Objects.equals(oldImageUrl, newImageUrl);

            if (imageChanged) {
                deleteExamImageIfExists(oldImageUrl);
            }

            choice.setChoiceLabel(request.getChoiceLabel());
            choice.setChoiceText(request.getChoiceText());
            choice.setChoiceImageUrl(newImageUrl);
            choice.setCorrect(request.getCorrect());
            choice.setChoiceOrder(order++);

            choiceRepository.save(choice);
        }
    }

    private void syncRubrics(
            ExamQuestion question,
            List<EssayRubricRequest> requests
    ) {

        List<EssayRubric> existingRubrics =
                essayRubricRepository
                        .findByQuestionQuestionIdOrderByDisplayOrderAsc(
                                question.getQuestionId()
                        );

        Map<Long, EssayRubric> existingMap =
                existingRubrics.stream()
                        .collect(Collectors.toMap(
                                EssayRubric::getRubricId,
                                r -> r
                        ));

        Set<Long> incomingIds = requests.stream()
                .map(EssayRubricRequest::getRubricId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (EssayRubric existing : existingRubrics) {

            if (!incomingIds.contains(existing.getRubricId())) {

                essayRubricRepository.delete(existing);
            }
        }

        int order = 1;

        for (EssayRubricRequest request : requests) {

            EssayRubric rubric;

            if (request.getRubricId() != null
                    && existingMap.containsKey(request.getRubricId())) {

                rubric = existingMap.get(request.getRubricId());

            } else {

                rubric = new EssayRubric();
                rubric.setQuestion(question);
            }

            rubric.setCriterionName(request.getCriterionName());
            rubric.setWeightPercentage(request.getWeightPercentage());
            rubric.setDescription(request.getDescription());
            rubric.setDisplayOrder(order++);

            essayRubricRepository.save(rubric);
        }
    }

    private void deleteExamImageIfExists(String imageUrl) {

        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        if (!imageUrl.startsWith("/uploads/exams/")) {
            return;
        }

        String filename = imageUrl.replace("/uploads/exams/", "");

        File file = new File(System.getProperty("user.dir") + "/uploads/exams/", filename);

        if (file.exists() && !file.delete()) {
        }
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String determineStatus(
            Exam exam,
            List<ExamAssignment> assignments
    ) {
        if (exam == null) {
            return "DRAFT";
        }

        List<String> classOfferingIds =
                assignments == null
                        ? List.of()
                        : assignments.stream()
                        .map(ExamAssignment::getClassOfferingId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        int totalAssignedStudents = classOfferingIds.isEmpty()
                ? 0
                : classEnrollmentCacheRepository
                .countDistinctEnrolledStudentsByClassOfferingIds(classOfferingIds);

        int submittedStudents =
                attemptRepository.countFinishedAttemptsByExamId(
                        exam.getExamId()
                );

        return examStatusService.getDisplayStatus(
                exam,
                totalAssignedStudents,
                submittedStudents
        ).name();
    }

    private String formatAssignedTo(
            List<ExamAssignment> assignments,
            Map<String, ClassOfferingMeta> metaMap
    ) {
        if (assignments == null || assignments.isEmpty()) {
            return "Unassigned";
        }

        return assignments.stream()
                .map(ExamAssignment::getClassOfferingId)
                .filter(Objects::nonNull)
                .map(metaMap::get)
                .filter(Objects::nonNull)
                .map(ClassOfferingMeta::displayName)
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