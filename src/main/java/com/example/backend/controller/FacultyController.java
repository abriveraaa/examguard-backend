package com.example.backend.controller;

import com.example.backend.dto.faculty.*;
import com.example.backend.dto.faculty.response.FacultyDashboardResponse;
import com.example.backend.service.faculty.FacultyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faculty")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<FacultyDashboardResponse> getFacultyDashboard(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                facultyService.getFacultyDashboard(employeeId, role)
        );
    }

    @GetMapping("/dashboard/profile")
    public ResponseEntity<FacultyProfileDTO> getDashboardProfile(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                facultyService.getDashboardProfile(employeeId, role)
        );
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<FacultyDashboardStatsDTO> getDashboardStats(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                facultyService.getDashboardStats(employeeId, role)
        );
    }

    @GetMapping("/dashboard/active-exams")
    public ResponseEntity<List<FacultyExamSummaryDTO>> getDashboardActiveExams(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                facultyService.getDashboardActiveExams(employeeId, role)
        );
    }

    @GetMapping("/dashboard/recent-submissions")
    public ResponseEntity<List<FacultySubmissionSummaryDTO>> getRecentSubmissions(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                facultyService.getDashboardRecentSubmissions(employeeId, role)
        );
    }

    @GetMapping("/dashboard/needs-review")
    public ResponseEntity<List<FacultyViolationReviewDTO>> getNeedsReview(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                facultyService.getDashboardNeedsReview(employeeId, role)
        );
    }

    @GetMapping("/dashboard/classes")
    public ResponseEntity<List<FacultyClassDTO>> getFacultyClasses(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                facultyService.getFacultyClasses(employeeId, role)
        );
    }
}