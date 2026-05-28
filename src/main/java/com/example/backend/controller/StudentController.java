package com.example.backend.controller;

import com.example.backend.dto.exam.request.ExamActivityRequest;
import com.example.backend.dto.exam.result.ExamResult;
import com.example.backend.dto.exam.request.SaveAnswerRequest;
import com.example.backend.dto.exam.request.SubmitExamRequest;
import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.dto.student.dashboard.StudentResponse;
import com.example.backend.dto.student.result.StudentExamResultResponse;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.entity.exam.Exam;
import com.example.backend.repository.exam.ExamRepository;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.report.StudentAnswerSheetReportService;
import com.example.backend.service.student.StudentExamService;
import com.example.backend.service.student.StudentService;
import com.example.backend.service.exam.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ExamService examTakingService;
    private final StudentExamService studentExamService;
    private final StudentAnswerSheetReportService studentAnswerSheetReportService;
    private final ExamRepository examRepository;
    private final AuthService authService;

    @GetMapping("/dashboard")
    public ResponseEntity<StudentResponse> getDashboard(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(studentService.getDashboard(user.getSchoolId(), user.getRole()));
    }

    @PostMapping("/dashboard/results/{examId}/view")
    public ResponseEntity<Void> markResultViewed(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        studentService.markDashboardItemViewed(
                user.getSchoolId(),
                "RESULT",
                examId
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/exams/answers/save")
    public ResponseEntity<ExamResult> saveAnswer(
            @RequestBody SaveAnswerRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examTakingService.saveAnswer(
                        request,
                        user.getSchoolId()
                )
        );
    }

    @GetMapping("/exams")
    public List<StudentExamCardDTO> getStudentExams(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Only students can access student exams.");
        }

        return studentExamService.getStudentExamCards(user.getSchoolId());
    }

    @GetMapping("/exams/{examId}/results")
    public ResponseEntity<StudentExamResultResponse> getStudentExamResult(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                studentService.getStudentExamResult(examId, user.getSchoolId())
        );
    }

    @GetMapping("/exams/{examId}/answer-sheet-report")
    public ResponseEntity<byte[]> downloadAnswerSheetReport(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        byte[] pdf = studentAnswerSheetReportService.generate(examId, user.getSchoolId());

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        String sanitizedExamTitle = exam.getTitle()
                .replaceAll("[^a-zA-Z0-9-_ ]", "")
                .trim()
                .replace(" ", "_");

        String filename =
                user.getSchoolId() + "-" + sanitizedExamTitle + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .body(pdf);
    }

    @PostMapping("/exams/activity")
    public ExamResult logExamActivity(
            @RequestBody ExamActivityRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return examTakingService.logExamActivity(request, user.getSchoolId(), user.getRole());
    }

    @PostMapping("/exams/submit")
    public ResponseEntity<ExamResult> submitExam(
            @RequestBody SubmitExamRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                examTakingService.submitExam(request.getAttemptId(), user.getSchoolId())
        );
    }
}