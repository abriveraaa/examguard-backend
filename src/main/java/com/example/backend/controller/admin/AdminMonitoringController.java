package com.example.backend.controller.admin;

import com.example.backend.dto.admin.monitoring.AdminMonitoringLogsRequest;
import com.example.backend.dto.admin.monitoring.AdminMonitoringLogsResponse;
import com.example.backend.dto.admin.monitoring.MonitoringFilterRequest;
import com.example.backend.dto.admin.monitoring.MonitoringOverviewResponse;
import com.example.backend.service.admin.AdminMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/admin/monitoring")
@RestController
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final AdminMonitoringService adminMonitoringService;

    @PostMapping("/overview")
    public ResponseEntity<MonitoringOverviewResponse> getOverview(
            @RequestBody MonitoringFilterRequest filter,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(adminMonitoringService.getOverview(filter));
    }

    @PostMapping("/logs")
    public ResponseEntity<AdminMonitoringLogsResponse> getLogs(
            @RequestBody AdminMonitoringLogsRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role
    ) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(adminMonitoringService.getLogs(request));
    }
}