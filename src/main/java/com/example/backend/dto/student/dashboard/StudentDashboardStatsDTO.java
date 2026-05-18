package com.example.backend.dto.student.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardStatsDTO {
    private Integer upcomingExamCount;
    private Integer completedExamCount;
    private Integer violationCount;
    private Double averageScore;
}
