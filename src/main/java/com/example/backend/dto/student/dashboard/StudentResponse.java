package com.example.backend.dto.student.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private StudentProfileDTO profile;
    private List<StudentUpcomingExamDTO> upcomingExams;
    private List<StudentResultSummaryDTO> resultSummary;
    private List<StudentViolationSummaryDTO> violations;
    private StudentDashboardStatsDTO stats;
}