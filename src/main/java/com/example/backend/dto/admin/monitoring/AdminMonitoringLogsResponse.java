package com.example.backend.dto.admin.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class AdminMonitoringLogsResponse implements Serializable {

    private List<AdminLogRowDto> content;
    private AdminMonitoringFilterOptionsDto filterOptions;

    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
}