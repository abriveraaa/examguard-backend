package com.example.backend.controller.faculty;

import com.example.backend.dto.faculty.reports.*;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.report.faculty.ExamResultSummaryService;
import com.example.backend.report.model.ReportType;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.faculty.FacultyReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.backend.report.model.ReportRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import java.util.List;

@RestController
@RequestMapping("/faculty/reports")
@RequiredArgsConstructor
public class FacultyReportsController {

    private final FacultyReportsService facultyReportsService;
    private final ExamResultSummaryService examResultSummaryService;
    private final AuthService authService;

    @GetMapping("/summary")
    public FacultyReportSummaryDTO getSummary(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String classOfferingId,
            @RequestParam(required = false) Long examId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyReportsService.getSummary(
                user.getSchoolId(),
                academicYear,
                term,
                courseCode,
                classOfferingId,
                examId
        );
    }

    @GetMapping("/participation")
    public List<ExamParticipationDTO> getParticipation(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String classOfferingId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyReportsService.getParticipation(
                user.getSchoolId(),
                academicYear,
                term,
                courseCode,
                classOfferingId
        );
    }

    @GetMapping("/submission-status")
    public List<SubmissionStatusDTO> getSubmissionStatus(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String classOfferingId,
            @RequestParam(required = false) Long examId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyReportsService.getSubmissionStatus(
                user.getSchoolId(),
                academicYear,
                term,
                courseCode,
                classOfferingId,
                examId
        );
    }

    @GetMapping("/submission-breakdown")
    public List<ExamSubmissionBreakdownDTO> getSubmissionBreakdown(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String classOfferingId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyReportsService.getSubmissionBreakdown(
                user.getSchoolId(),
                academicYear,
                term,
                courseCode,
                classOfferingId
        );
    }

    @GetMapping("/violations")
    public List<ViolationTypeDTO> getViolations(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String classOfferingId,
            @RequestParam(required = false) Long examId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyReportsService.getViolations(
                user.getSchoolId(),
                academicYear,
                term,
                courseCode,
                classOfferingId,
                examId
        );
    }

    @GetMapping("/exams")
    public List<ReportExamOptionDTO> getExamOptions(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String classOfferingId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyReportsService.getExamOptions(
                user.getSchoolId(),
                academicYear,
                term,
                courseCode,
                classOfferingId
        );
    }

    @GetMapping("/exams/{examId}/result-summary-report")
    public ResponseEntity<byte[]> downloadExamResultSummaryReport(
            @PathVariable Long examId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String classOfferingId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        ReportRequest request = ReportRequest.builder()
                .reportType(ReportType.EXAM_RESULT_SUMMARY)
                .examId(examId)
                .classOfferingId(classOfferingId)
                .generatedByText(
                        "Generated By: "
                                + user.getSchoolId()
                                + " | "
                                + LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .build();

        byte[] pdf = examResultSummaryService.generatePdf(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=exam-result-summary.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/class-record/export/pdf")
    public ResponseEntity<byte[]> downloadClassRecordPdf(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String classOfferingId
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        byte[] pdf = facultyReportsService.exportClassRecordPdf(user.getSchoolId(),classOfferingId);

        String filename = "class-record-" + "-" + classOfferingId + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}