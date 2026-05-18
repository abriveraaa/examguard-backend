package com.example.backend.service.core;

import com.example.backend.entity.core.ReactivationLog;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.core.UserAccessRepository;
import com.example.backend.repository.core.UserSessionLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReactivationService {

    private final UserAccessRepository userAccessRepository;
    private final AccountStatusLogService accountStatusLogService;
    private final UserSessionLogRepository userSessionLogRepository;

    public ReactivationService(UserAccessRepository userAccessRepository,
                               AccountStatusLogService accountStatusLogService,
                               UserSessionLogRepository userSessionLogRepository) {
        this.userAccessRepository = userAccessRepository;
        this.accountStatusLogService = accountStatusLogService;
        this.userSessionLogRepository = userSessionLogRepository;
    }

    // =========================
    // BULK REACTIVATE
    // =========================
    public String bulkReactivateEligibleUsers(String justification) {

        List<UserAccess> users = userAccessRepository.findByEligibleForReactivationTrue();

        if (users.isEmpty()) {
            return "No users eligible for reactivation.";
        }

        int count = 0;

        for (UserAccess user : users) {
            if (user == null) continue;

            user.setActive(true);
            user.setBlocked(false);
            user.setDeactivationReason(null);
            user.setEligibleForReactivation(false);

            userAccessRepository.save(user);

            accountStatusLogService.log(
                    user,
                    "REACTIVATED",
                    justification,
                    "ADMIN",
                    "INACTIVE",
                    "ACTIVE"
            );

            logReactivation(user, justification);
            count++;
        }

        return count + " user(s) reactivated successfully.";
    }

    // =========================
    // SINGLE REACTIVATE
    // =========================
    public String reactivateSingleUser(
            String schoolId,
            String role,
            String justification,
            UserAccess performedBy
    ) {
        Optional<UserAccess> userOpt = userAccessRepository.findBySchoolId(safe(schoolId));

        if (userOpt.isEmpty()) {
            return "User not found.";
        }

        UserAccess user = userOpt.get();

        if (user.isActive() && !user.isBlocked()) {
            return "User is already active.";
        }

        if (performedBy == null || performedBy.getUsername() == null || performedBy.getUsername().isBlank()) {
            return "Session user not found. Please login again.";
        }

        String previousStatus = user.isBlocked()
                ? "BLOCKED"
                : (user.isActive() ? "ACTIVE" : "INACTIVE");

        user.setActive(true);
        user.setBlocked(false);
        user.setDeactivationReason(null);
        user.setEligibleForReactivation(false);

        userAccessRepository.save(user);

        accountStatusLogService.log(
                user,
                "REACTIVATED",
                justification,
                performedBy.getUsername(),
                previousStatus,
                "ACTIVE"
        );

        return "User reactivated successfully.";
    }

    // =========================
    // HELPER
    // =========================
    private void logReactivation(UserAccess user, String justification) {

        ReactivationLog log = new ReactivationLog();
        log.setSchoolId(user.getSchoolId());
        log.setRole(user.getRole());
        log.setJustification(
                justification == null || justification.isBlank()
                        ? "Manual reactivation"
                        : justification
        );

    }

    public UserAccess getUserFromSession(String sessionToken) {

        return userSessionLogRepository
                .findBySessionToken(sessionToken)
                .filter(s -> "ACTIVE".equals(s.getLoginStatus()))
                .map(s -> s.getUserAccess())
                .orElse(null);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}