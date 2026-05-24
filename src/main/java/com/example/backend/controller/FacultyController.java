package com.example.backend.controller;

import com.example.backend.dto.faculty.*;
import com.example.backend.dto.faculty.response.FacultyDashboardResponse;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.exam.ExamService;
import com.example.backend.service.faculty.FacultyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faculty")
public class FacultyController {

    private final FacultyService facultyService;
    private final AuthService authService;

    public FacultyController(FacultyService facultyService,
                             AuthService authService) {
        this.facultyService = facultyService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<FacultyDashboardResponse> getFacultyDashboard(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(facultyService.getFacultyDashboard(user.getSchoolId(), user.getRole()));
    }

    @GetMapping("/dashboard/profile")
    public ResponseEntity<FacultyProfileDTO> getDashboardProfile(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                facultyService.getDashboardProfile(user.getSchoolId(), user.getRole()));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<FacultyDashboardStatsDTO> getDashboardStats(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                facultyService.getDashboardStats(user.getSchoolId(), user.getRole()));
    }

    @GetMapping("/dashboard/active-exams")
    public ResponseEntity<List<FacultyExamSummaryDTO>> getDashboardActiveExams(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                facultyService.getDashboardActiveExams(user.getSchoolId(), user.getRole()));
    }

    @GetMapping("/dashboard/recent-submissions")
    public ResponseEntity<List<FacultySubmissionSummaryDTO>> getRecentSubmissions(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                facultyService.getDashboardRecentSubmissions(user.getSchoolId(), user.getRole()));
    }

    @GetMapping("/dashboard/needs-review")
    public ResponseEntity<List<FacultyViolationReviewDTO>> getNeedsReview(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                facultyService.getDashboardNeedsReview(user.getSchoolId(), user.getRole()));
    }

    @GetMapping("/dashboard/classes")
    public ResponseEntity<List<FacultyClassDTO>> getFacultyClasses(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return ResponseEntity.ok(
                facultyService.getFacultyClasses(user.getSchoolId(), user.getRole()));
    }
}