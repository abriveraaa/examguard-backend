package com.example.backend.controller;

import com.example.backend.dto.exam.request.ExamActivityRequest;
import com.example.backend.dto.exam.result.ExamResult;
import com.example.backend.dto.exam.request.SaveAnswerRequest;
import com.example.backend.dto.exam.request.SubmitExamRequest;
import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.dto.student.dashboard.StudentResponse;
import com.example.backend.dto.student.result.StudentExamResultResponse;
import com.example.backend.entity.exam.Exam;
import com.example.backend.repository.exam.ExamRepository;
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

    @GetMapping("/dashboard")
    public ResponseEntity<StudentResponse> getDashboard(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                studentService.getDashboard(userId, role)
        );
    }

    @PostMapping("/dashboard/results/{examId}/view")
    public ResponseEntity<Void> markResultViewed(
            @PathVariable Long examId,
            @RequestHeader("X-User-Id") String studentId
    ) {
        studentService.markDashboardItemViewed(
                studentId,
                "RESULT",
                examId
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/dashboard/violations/{examId}/view")
    public ResponseEntity<Void> markViolationViewed(
            @PathVariable Long examId,
            @RequestHeader("X-User-Id") String studentId
    ) {
        studentService.markDashboardItemViewed(
                studentId,
                "VIOLATION",
                examId
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/exams/answers/save")
    public ResponseEntity<ExamResult> saveAnswer(
            @RequestBody SaveAnswerRequest request,
            @RequestHeader("X-User-Id") String studentId
    ) {

        return ResponseEntity.ok(
                examTakingService.saveAnswer(
                        request,
                        studentId
                )
        );
    }

    @GetMapping("/exams")
    public List<StudentExamCardDTO> getStudentExams(
            @RequestHeader("X-User-Id") String studentId,
            @RequestHeader("X-Role") String role
    ) {
        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only students can access student exams.");
        }

        return studentExamService.getStudentExamCards(studentId);
    }

    @GetMapping("/exams/{examId}/results")
    public ResponseEntity<StudentExamResultResponse> getStudentExamResult(
            @PathVariable Long examId,
            @RequestHeader("X-User-Id") String studentId
    ) {
        return ResponseEntity.ok(
                studentService.getStudentExamResult(examId, studentId)
        );
    }

    @GetMapping("/exams/{examId}/answer-sheet-report")
    public ResponseEntity<byte[]> downloadAnswerSheetReport(
            @PathVariable Long examId,
            @RequestHeader("X-User-Id") String studentId
    ) {

        byte[] pdf = studentAnswerSheetReportService.generate(examId, studentId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        String sanitizedExamTitle = exam.getTitle()
                .replaceAll("[^a-zA-Z0-9-_ ]", "")
                .trim()
                .replace(" ", "_");

        String filename =
                studentId + "-" + sanitizedExamTitle + ".pdf";

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
            @RequestHeader("X-User-Id") String studentId,
            @RequestHeader("X-Role") String role
    ) {
        return examTakingService.logExamActivity(request, studentId, role);
    }

    @PostMapping("/exams/submit")
    public ResponseEntity<ExamResult> submitExam(
            @RequestBody SubmitExamRequest request,
            @RequestHeader("X-User-Id") String studentId
    ) {
        return ResponseEntity.ok(
                examTakingService.submitExam(request.getAttemptId(), studentId)
        );
    }
}