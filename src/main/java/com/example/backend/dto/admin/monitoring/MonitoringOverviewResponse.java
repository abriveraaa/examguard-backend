package com.example.backend.dto.admin.monitoring;

import lombok.Data;

import java.util.List;

@Data
public class MonitoringOverviewResponse {

    private List<MetricCardDto> summaryCards;
    private List<ChartPointDto> concurrentUsersByRole;
    private List<ChartPointDto> activityVolume;
    private List<ChartPointDto> violationsByType;
    private List<ChartPointDto> violationsByProgram;
    private List<ChartPointDto> activeSessionsByRole;
    private List<AdminLogRowDto> recentCriticalEvents;
    private List<ChartPointDto> loginVolume;
    private List<ChartPointDto> concurrentUsers;
}