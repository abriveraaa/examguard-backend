package com.example.backend.dto.profile;

import java.util.List;

public record ProfileResponseDTO(
        String role,
        String fullName,
        String schoolEmail,
        String schoolId,
        String collegeOrOffice,
        String programOrPosition,
        String username,
        String accountStatus,
        String memberSince,
        String tenureDuration,
        String passwordStatus,
        String passwordLastChanged,
        String profileImageUrl,
        String currentAcademicYear,
        String currentTerm,
        List<ProfileClassDTO> classes,
        List<ProfileActivityDTO> recentActivities
) {}