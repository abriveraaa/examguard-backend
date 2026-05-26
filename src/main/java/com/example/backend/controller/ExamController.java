package com.example.backend.controller;

import com.example.backend.dto.exam.request.*;
import com.example.backend.dto.exam.response.ClassOfferingResponse;
import com.example.backend.dto.exam.response.ExamResponse;
import com.example.backend.dto.exam.response.ExamTakingResponse;
import com.example.backend.dto.exam.response.ImageUploadResponse;
import com.example.backend.dto.exam.result.AssignExamResult;
import com.example.backend.dto.exam.result.ExamResult;
import com.example.backend.dto.faculty.*;
import com.example.backend.dto.faculty.request.FacultyUpdateAnswerScoreRequest;
import com.example.backend.dto.faculty.request.ViolationDecisionRequest;
import com.example.backend.dto.faculty.response.AnswerReviewTimelineDTO;
import com.example.backend.dto.faculty.response.FacultyAttemptReviewResponse;
import com.example.backend.dto.faculty.response.FacultyExamDetailResponse;
import com.example.backend.dto.faculty.response.SimpleMessageResponse;
import com.example.backend.entity.cache.FacultyProfileCache;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.AdminProfile;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.report.exam.dto.ReportExportResult;
import com.example.backend.report.model.ReportExportMode;
import com.example.backend.repository.cache.FacultyProfileCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.core.SystemActivityLogService;
import com.example.backend.service.exam.ExamAssignmentService;
import com.example.backend.service.exam.ExamService;
import com.example.backend.service.exam.ExamTemplateService;
import com.example.backend.service.exam.ExamWorkspaceService;
import com.example.backend.service.report.ReportExportService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exams")
@AllArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final ExamAssignmentService examAssignmentService;
    private final ExamWorkspaceService examWorkspaceService;
    private final ExamTemplateService examTemplateService;
    private final ReportExportService reportExportService;
    private final SystemActivityLogService activityLogService;
    private final AdminProfileRepository adminProfileRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final AuthService authService;

    // =========================
    // TEMPLATE
    // =========================

    @GetMapping("/template/download")
    public ResponseEntity<Resource> downloadExamTemplate() {
        Resource resource = new ClassPathResource("templates/exam_template.xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=exam_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/template/preview")
    public ResponseEntity<?> previewTemplate(@RequestParam("file") MultipartFile file) {
        try {
            List<QuestionRequest> questions = examTemplateService.parseTemplate(file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Template parsed successfully.",
                    "questions", questions
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "questions", List.of()
            ));
        }
    }

    // =========================
    // IMAGES
    // =========================

    @PostMapping("/images/upload")
    public ResponseEntity<ImageUploadResponse> uploadExamImage(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            ImageUploadResponse response = examService.uploadExamImage(file);

            if (!response.isSuccess()) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    new ImageUploadResponse(false, "Failed to upload image.", null)
            );
        }
    }

    @PostMapping("/evidence/upload")
    public ResponseEntity<ImageUploadResponse> uploadEvidence(
            @RequestParam("file") MultipartFile file
    ){
        try{
            ImageUploadResponse response = examService.uploadViolationEvidence(file);

            if (!response.isSuccess()) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    new ImageUploadResponse(false, "Failed to upload image.", null)
            );
        }

    }

    // =========================
    // CREATE
    // =========================
    @PostMapping("/draft")
    public ResponseEntity<ExamResult> examDraft(
            @RequestBody ExamRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ExamResult result = examService.examDraft(request, user.getSchoolId(), user.getRole());

        ResponseEntity<ExamResult> response = buildResponse(result);

        return response;
    }

    @PostMapping("/publish")
    public ResponseEntity<ExamResult> examPublish(
            @RequestBody ExamRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ExamResult result = examService.examPublish(request, user.getSchoolId(), user.getRole());
        return buildResponse(result);
    }

    // =========================
    // PREVIEW
    // =========================

    @PutMapping("/{examId}")
    public ResponseEntity<ExamResult> updateExam(
            @PathVariable Long examId,
            @RequestBody ExamRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired session."
            );
        }

        ExamResult result = examService.updateExam(examId, request, user.getSchoolId(), user.getRole());

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamResponse> viewExam(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired session."
            );
        }

        return ResponseEntity.ok(
                examService.viewExam(
                        examId,
                        user.getSchoolId(),
                        user.getRole()
                )
        );
    }

    @GetMapping("/{examId}/taking")
    public ResponseEntity<?> getExamForTaking(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ExamTakingResponse response =
                examService.getExamForTaking(examId, user.getSchoolId(), user.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{examId}/begin")
    public ResponseEntity<?> beginExam(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ExamTakingResponse response =
                examService.beginExamAttempt(examId, user.getSchoolId(), user.getRole());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{examId}/workspace")
    public ResponseEntity<FacultyExamDetailResponse> getExamWorkspace(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examWorkspaceService.getExamDetail(examId, user.getSchoolId(), user.getRole())
        );
    }

    @GetMapping("/{examId}/students")
    public ResponseEntity<List<FacultyExamStudentDTO>> getExamStudents(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examWorkspaceService.getExamStudents(examId, user.getSchoolId(), user.getRole())
        );
    }

    @GetMapping("/{examId}/students/{studentId}/review")
    public ResponseEntity<FacultyAttemptReviewResponse> getStudentAttemptReview(
            @PathVariable Long examId,
            @PathVariable String studentId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                examWorkspaceService.getStudentAttemptReview(examId, studentId, userId, role)
        );
    }

    @GetMapping("/{examId}/submissions")
    public ResponseEntity<List<FacultySubmissionSummaryDTO>> getExamSubmissions(
            @PathVariable Long examId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                examWorkspaceService.getExamSubmissions(examId, userId, role)
        );
    }

    @GetMapping("/{examId}/violations")
    public ResponseEntity<List<FacultyStudentViolationDTO>> getExamViolations(
            @PathVariable Long examId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                examWorkspaceService.getExamViolations(examId, userId, role)
        );
    }

    @PostMapping("/review/violations/decision")
    public SimpleMessageResponse applyViolationDecision(
            @RequestBody ViolationDecisionRequest request,
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return examService.applyViolationDecision(
                request,
                employeeId,
                role
        );
    }

    @GetMapping("/review/answers/{answerId}/timeline")
    public ResponseEntity<List<AnswerReviewTimelineDTO>> getAnswerReviewTimeline(
            @PathVariable Long answerId,
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                examWorkspaceService.getAnswerReviewTimeline(answerId, employeeId, role)
        );
    }

    @GetMapping("/{examId}/leaderboard")
    public ResponseEntity<List<FacultyLeaderboardDTO>> getExamLeaderboard(
            @PathVariable Long examId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                examWorkspaceService.getExamLeaderboard(examId, userId, role)
        );
    }

    @PutMapping("/{examId}/results/release")
    public ResponseEntity<SimpleMessageResponse> releaseExamResults(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examWorkspaceService.releaseExamResults(examId, user.getSchoolId(), user.getRole())
        );
    }

    @GetMapping("/{examId}/activity")
    public ResponseEntity<List<FacultyActivityLogDTO>> getExamActivityLogs(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examWorkspaceService.getExamActivityLogs(examId, user.getSchoolId(), user.getRole())
        );
    }

    // ========================
    // SCORES
    // ========================

    @PostMapping("/answers/{answerId}/score")
    public ResponseEntity<SimpleMessageResponse> updateAnswerScore(
            @PathVariable Long answerId,
            @RequestBody FacultyUpdateAnswerScoreRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examWorkspaceService.updateAnswerScore(
                        answerId,
                        request.getPointsAwarded(),
                        user.getSchoolId(),
                        user.getRole()
                )
        );
    }

    @PostMapping("/answers/essay-review")
    public ResponseEntity<ExamResult> saveEssayReview(
            @RequestBody FacultyEssayReviewRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examService.saveEssayReview(request, user.getSchoolId(), user.getRole())
        );
    }

    @PostMapping("/attempts/{attemptId}/mark-reviewed")
    public ResponseEntity<SimpleMessageResponse> markAttemptReviewed(
            @PathVariable Long attemptId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examWorkspaceService.markAttemptReviewed(
                        attemptId,
                        user.getSchoolId(),
                        user.getRole()
                )
        );
    }

    // =========================
    // FETCH ALL
    // =========================
    @GetMapping
    public ResponseEntity<List<ExamResponse>> getAllExams(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired session."
            );
        }

        return ResponseEntity.ok(examService.getAllExams(user.getRole(), user.getSchoolId()));
    }

    // =========================
    // UPLOAD TEMPLATE
    // =========================
    @PostMapping("/{examId}/upload")
    public ResponseEntity<ExamResult> uploadExam(
            @PathVariable Long examId,
            @RequestParam("file") MultipartFile file
    ) {
        ExamResult result = examService.replaceUploadExamTemplate(examId, file);
        return buildResponse(result);
    }

    // =========================
    // ASSIGN
    // =========================
    @PostMapping("/{examId}/assign")
    public ResponseEntity<AssignExamResult> assignExam(
            @PathVariable Long examId,
            @RequestBody AssignExamRequest request
    ) {
        AssignExamResult result = examAssignmentService.assignExam(examId, request);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    // =========================
    // STATE CHANGES
    // =========================
    @PutMapping("/{examId}/cancel")
    public ResponseEntity<ExamResult> cancelExam(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ExamResult result = examService.cancelExam(examId, user.getSchoolId(), user.getRole());
        return buildResponse(result);
    }

    @PutMapping("/{examId}/restore")
    public ResponseEntity<ExamResult> restoreExam(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ExamResult result = examService.restoreExam(examId, user.getSchoolId(), user.getRole());
        return buildResponse(result);
    }

    @PutMapping("/{examId}/publish")
    public ResponseEntity<ExamResult> publishExamById(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ExamResult result = examService.publishExamById(examId, user.getSchoolId(), user.getRole());
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    // =========================
    // CLASS OFFERINGS
    // =========================
    @GetMapping("/class-offerings")
    public ResponseEntity<List<ClassOfferingResponse>> getClassOfferings(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(examService.getClassOfferings(user.getSchoolId(), user.getRole()));
    }

    // =========================
    // REPORTS
    // =========================


    @GetMapping("/{examId}/portfolio")
    public ResponseEntity<byte[]> downloadPortfolio(
            @PathVariable Long examId,
            @RequestParam(required = false) String classOfferingId,
            @RequestParam(defaultValue = "MERGED") String mode,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {

        long start = System.currentTimeMillis();

        if (!"ADMIN".equalsIgnoreCase(role)
                && !"FACULTY".equalsIgnoreCase(role)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admin and faculty can download exam portfolio reports."
            );
        }

        ReportExportMode exportMode;

        try {
            exportMode = ReportExportMode.valueOf(mode.trim().toUpperCase());
        } catch (Exception e) {
            exportMode = ReportExportMode.MERGED;
        }

        ReportExportResult result = reportExportService.generateExamPortfolioPdf(
                examId,
                exportMode,
                classOfferingId,
                buildGeneratedByText(userId, role)
        );

        long durationMs = System.currentTimeMillis() - start;

        activityLogService.log(
                userId,
                role,
                "REPORTS",
                "GENERATE_EXAM_PORTFOLIO",
                "SUCCESS",
                "Generated exam portfolio report. Mode: " + exportMode,
                examId,
                null,
                null,
                durationMs
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\""
                )
                .contentType(result.contentType())
                .body(result.bytes());
    }

    // =========================
    // LOG
    // =========================
    @PostMapping("/violations")
    public ResponseEntity<?> logViolation(@RequestBody ViolationLogRequest request) {
        examService.logViolation(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Violation logged."
        ));
    }

    // =========================
    // HELPER
    // =========================
    private ResponseEntity<ExamResult> buildResponse(ExamResult result) {
        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    private String buildGeneratedByText(
            String schoolId,
            String role
    ) {

        String fullName = schoolId;

        try {

            if ("ADMIN".equalsIgnoreCase(role)) {

                AdminProfile admin =
                        adminProfileRepository
                                .findByEmployeeId(schoolId)
                                .orElse(null);

                if (admin != null) {
                    fullName =
                            admin.getFirstName()
                                    + " "
                                    + admin.getLastName();
                }

            } else if ("FACULTY".equalsIgnoreCase(role)) {

                FacultyProfileCache faculty =
                        facultyProfileCacheRepository
                                .findByEmployeeId(schoolId)
                                .orElse(null);

                if (faculty != null) {
                    fullName =
                            faculty.getFirstName()
                                    + " "
                                    + faculty.getLastName();
                }

            } else if ("STUDENT".equalsIgnoreCase(role)) {

                StudentProfileCache student =
                        studentProfileCacheRepository
                                .findByStudentId(schoolId)
                                .orElse(null);

                if (student != null) {
                    fullName =
                            student.getFirstName()
                                    + " "
                                    + student.getLastName();
                }
            }

        } catch (Exception ignored) {
        }

        return "ExamGuard | Generated: "
                + schoolId
                + " | "
                + fullName
                + " | "
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}