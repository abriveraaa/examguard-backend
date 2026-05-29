package com.example.backend.dto.admin.users;

import lombok.Data;

import java.io.Serializable;

@Data
public class AdminUsersExportRequest implements Serializable {

    private String role;
    private String format;
    private String keyword;
    private String status;
    private String reactivation;
}