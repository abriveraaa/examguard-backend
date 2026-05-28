package com.example.backend.dto.admin.users;

import lombok.Data;

@Data
public class AdminUsersExportRequest {

    private String role;
    private String format;
    private String keyword;
    private String status;
    private String reactivation;
}