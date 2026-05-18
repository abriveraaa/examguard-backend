package com.example.backend.dto.faculty.response;

import com.example.backend.dto.faculty.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class FacultyDashboardResponse {

    private FacultyProfileDTO profile;
    private FacultyDashboardStatsDTO stats;

    private List<FacultyExamSummaryDTO> activeExams;
    private List<FacultyViolationReviewDTO> needsReview;
    private List<FacultySubmissionSummaryDTO> recentSubmissions;
}