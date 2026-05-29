package com.example.backend.dto.admin.monitoring;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminMonitoringFilterOptionsDto implements Serializable {

    private List<String> roles = new ArrayList<>();
    private List<String> statuses = new ArrayList<>();
    private List<String> actions = new ArrayList<>();
    private List<String> modules = new ArrayList<>();
    private List<String> severities = new ArrayList<>();
    private List<String> violationTypes = new ArrayList<>();

    private List<String> cameraStatuses = new ArrayList<>();
    private List<String> cameraDeviceTypes = new ArrayList<>();
    private List<String> cameraStreamRoles = new ArrayList<>();
}