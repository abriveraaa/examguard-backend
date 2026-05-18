package com.example.backend.service.core;

import com.example.backend.dto.core.AdminUserResponse;
import com.example.backend.dto.core.CreateAdminProfileRequest;
import com.example.backend.dto.core.UpdateAdminProfileRequest;
import com.example.backend.entity.core.AdminProfile;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.repository.core.UserAccessRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminProfileService {

    private final AdminProfileRepository adminProfileRepository;
    private final UserAccessRepository userAccessRepository;
    private final AccountStatusLogService accountStatusLogService;

    public AdminProfileService(AdminProfileRepository adminProfileRepository,
                               UserAccessRepository userAccessRepository,
                               AccountStatusLogService accountStatusLogService) {
        this.adminProfileRepository = adminProfileRepository;
        this.userAccessRepository = userAccessRepository;
        this.accountStatusLogService = accountStatusLogService;
    }

    public String createAdminProfile(CreateAdminProfileRequest request) {
        String employeeId = sanitize(request.getEmployeeId()).toUpperCase();
        String email = sanitize(request.getEmail()).toLowerCase();

        if (employeeId.isBlank()
                || sanitize(request.getFirstName()).isBlank()
                || sanitize(request.getLastName()).isBlank()
                || email.isBlank()
                || sanitize(request.getBirthDate()).isBlank()) {
            return "Please complete all required fields.";
        }

        if (adminProfileRepository.findByEmployeeId(employeeId).isPresent()) {
            return "Employee ID already exists.";
        }

        if (adminProfileRepository.findByEmail(email).isPresent()) {
            return "Email already exists.";
        }

        AdminProfile admin = new AdminProfile();
        admin.setEmployeeId(employeeId);
        admin.setFirstName(sanitize(request.getFirstName()));
        admin.setLastName(sanitize(request.getLastName()));
        admin.setEmail(email);
        admin.setBirthDate(LocalDate.parse(request.getBirthDate()));
        admin.setIsActive(true);

        adminProfileRepository.save(admin);

        return "Admin profile created successfully.";
    }

    public List<AdminUserResponse> getAllAdminProfiles() {
        return adminProfileRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AdminUserResponse getAdminProfileByEmployeeId(String employeeId) {
        String safeEmployeeId = sanitize(employeeId).toUpperCase();

        Optional<AdminProfile> adminOpt = adminProfileRepository.findByEmployeeId(safeEmployeeId);
        return adminOpt.map(this::toResponse).orElse(null);
    }

    public String updateAdminProfile(String employeeId, UpdateAdminProfileRequest request) {
        String safeEmployeeId = sanitize(employeeId).toUpperCase();

        Optional<AdminProfile> adminOpt = adminProfileRepository.findByEmployeeId(safeEmployeeId);
        if (adminOpt.isEmpty()) {
            return "Admin profile not found.";
        }

        AdminProfile admin = adminOpt.get();

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            admin.setFirstName(sanitize(request.getFirstName()));
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            admin.setLastName(sanitize(request.getLastName()));
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = sanitize(request.getEmail()).toLowerCase();

            Optional<AdminProfile> emailOwner = adminProfileRepository.findByEmail(newEmail);
            if (emailOwner.isPresent() && !emailOwner.get().getAdminId().equals(admin.getAdminId())) {
                return "Email is already used by another admin.";
            }

            admin.setEmail(newEmail);

            Optional<UserAccess> userOpt = userAccessRepository.findBySchoolId(admin.getEmployeeId());
            userOpt.ifPresent(user -> {
                user.setEmail(newEmail);
                userAccessRepository.save(user);
            });
        }

        if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
            admin.setBirthDate(LocalDate.parse(request.getBirthDate()));
        }

        if (request.getIsActive() != null) {
            admin.setIsActive(request.getIsActive());

            Optional<UserAccess> userOpt = userAccessRepository.findBySchoolId(admin.getEmployeeId());
            userOpt.ifPresent(user -> {
                user.setActive(request.getIsActive());
                userAccessRepository.save(user);
            });
        }

        adminProfileRepository.save(admin);

        return "Admin profile updated successfully.";
    }

    public String deactivateAdminProfile(String employeeId, String currentAdminId, String reason) {
        String safeEmployeeId = sanitize(employeeId).toUpperCase();
        String safeCurrentAdminId = sanitize(currentAdminId).toUpperCase();

        if (safeEmployeeId.equalsIgnoreCase(safeCurrentAdminId)) {
            return "You cannot deactivate your own account.";
        }

        Optional<AdminProfile> adminOpt = adminProfileRepository.findByEmployeeId(safeEmployeeId);

        if (adminOpt.isEmpty()) {
            return "Admin profile not found.";
        }

        AdminProfile profile = adminOpt.get();

        if (!Boolean.TRUE.equals(profile.getIsActive())) {
            return "Admin is already deactivated.";
        }

        long activeAdmins = adminProfileRepository.countByIsActiveTrue();

        if (activeAdmins <= 1) {
            return "Cannot deactivate the last active admin.";
        }

        profile.setIsActive(false);
        profile.setDeactivationReason(reason);
        profile.setDeactivatedAt(OffsetDateTime.now());

        adminProfileRepository.save(profile);

        Optional<UserAccess> userOpt = userAccessRepository.findBySchoolId(safeEmployeeId);

        if (userOpt.isPresent()) {
            UserAccess user = userOpt.get();

            user.setActive(false);
            user.setBlocked(false);
            user.setDeactivationReason(reason);

            userAccessRepository.save(user);

            accountStatusLogService.log(
                    user,
                    "DEACTIVATED",
                    reason,
                    safeCurrentAdminId.isBlank() ? "SYSTEM" : safeCurrentAdminId,
                    "ACTIVE",
                    "INACTIVE"
            );
        }

        return "Admin deactivated successfully.";
    }

    public String reactivateAdminProfile(String employeeId, UserAccess performedBy) {

        String safeEmployeeId = sanitize(employeeId).toUpperCase();

        Optional<AdminProfile> adminOpt = adminProfileRepository.findByEmployeeId(safeEmployeeId);

        if (adminOpt.isEmpty()) {
            return "Admin profile not found.";
        }

        AdminProfile profile = adminOpt.get();

        if (Boolean.TRUE.equals(profile.getIsActive())) {
            return "Admin is already active.";
        }

        profile.setIsActive(true);
        profile.setDeactivationReason(null);
        profile.setDeactivatedAt(null);

        adminProfileRepository.save(profile);

        Optional<UserAccess> userOpt = userAccessRepository.findBySchoolId(safeEmployeeId);

        if (userOpt.isEmpty()) {
            return "User access not found for admin.";
        }

        UserAccess user = userOpt.get();

        String previousStatus = user.isBlocked()
                ? "BLOCKED"
                : (user.isActive() ? "ACTIVE" : "INACTIVE");

        user.setActive(true);
        user.setBlocked(false);

        userAccessRepository.save(user);

        if (performedBy == null || performedBy.getUsername() == null || performedBy.getUsername().isBlank()) {
            return "Session user not found. Please login again.";
        }

        String performedByUsername = performedBy.getUsername();

        accountStatusLogService.log(
                user,
                "REACTIVATED",
                "Manual admin reactivation",
                performedByUsername,
                previousStatus,
                "ACTIVE"
        );

        return "Admin reactivated successfully.";
    }

    private AdminUserResponse toResponse(AdminProfile admin) {
        String registrarStatus = Boolean.TRUE.equals(admin.getIsActive()) ? "Active" : "Inactive";

        Optional<UserAccess> userOpt = userAccessRepository.findBySchoolId(admin.getEmployeeId());

        String systemAccess;
        if (userOpt.isEmpty()) {
            systemAccess = "Not Activated";
        } else if (!userOpt.get().isActive()) {
            systemAccess = "Inactive";
        } else if (userOpt.get().isBlocked()) {
            systemAccess = "Blocked";
        } else {
            systemAccess = "Active";
        }

        return new AdminUserResponse(
                admin.getEmployeeId(),
                "-",
                admin.getFirstName(),
                admin.getLastName(),
                admin.getEmail(),
                admin.getBirthDate(),
                registrarStatus,
                systemAccess
        );
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
}