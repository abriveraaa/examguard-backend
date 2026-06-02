package com.example.backend.service.auth;

import com.example.backend.dto.auth.LoginResult;
import com.example.backend.dto.core.*;
import com.example.backend.entity.cache.FacultyProfileCache;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.*;
import com.example.backend.repository.cache.FacultyProfileCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.*;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.core.AccountStatusLogService;
import com.example.backend.service.core.EmailService;
import com.example.backend.audit.ActivityTarget;
import com.example.backend.audit.ActivityTargetType;
import com.example.backend.audit.TrackActivity;
import com.example.backend.utility.TimeUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserAccessRepository userAccessRepository;
    private final UserAccessLogRepository userAccessLogRepository;
    private final UserSessionLogRepository userSessionLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProfileRepository adminProfileRepository;
    private final EmailService emailService;
    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final AccountStatusLogService accountStatusLogService;


    @TrackActivity(
            module = "AUTH",
            action = "ACTIVATE_ACCOUNT",
            message = "User attempted account activation"
    )
    public ActivationResult activateAccount(
            @ActivityTarget(ActivityTargetType.TARGET_USER_ID)
            String schoolId,
            String email,
            String birthday,
            String ipAddress
    ) {
        String originalSchoolId = sanitizeSchoolId(schoolId);
        String cleanedEmail = email == null ? "" : email.trim();
        String cleanedBirthday = normalizeDateString(birthday);
        String normalizedUsername = normalizeId(originalSchoolId);

        if (originalSchoolId.isEmpty() || cleanedEmail.isEmpty() || cleanedBirthday.isEmpty()) {
            return new ActivationResult(false, "Please complete all required fields.", null);
        }

        if (userAccessRepository.findByUsername(normalizedUsername).isPresent()
                || userAccessRepository.findBySchoolId(originalSchoolId).isPresent()) {
            return new ActivationResult(false, "Account already activated. Please log in or use Forgot Password.", null);
        }

        Optional<AdminProfile> adminOpt = adminProfileRepository.findByEmployeeId(originalSchoolId);
        if (adminOpt.isPresent()) {
            AdminProfile admin = adminOpt.get();

            boolean emailMatch = admin.getEmail() != null
                    && admin.getEmail().trim().equalsIgnoreCase(cleanedEmail);

            boolean birthDateMatch = admin.getBirthDate() != null
                    && admin.getBirthDate().toString().equals(cleanedBirthday);

            boolean profileActive = Boolean.TRUE.equals(admin.getIsActive());

            if (emailMatch && birthDateMatch) {
                if (!profileActive) {
                    return new ActivationResult(false, "Admin profile is inactive.", null);
                }

                return createActivatedAccount(
                        originalSchoolId,
                        normalizedUsername,
                        admin.getEmail(),
                        "ADMIN",
                        ipAddress
                );
            }
        }

        Optional<StudentProfileCache> studentOpt =
                studentProfileCacheRepository.findByStudentIdAndEmailAddressAndBirthDate(
                        originalSchoolId,
                        cleanedEmail,
                        cleanedBirthday
                );

        if (studentOpt.isPresent()) {
            StudentProfileCache student = studentOpt.get();

            String scholasticStatus = student.getScholasticStatus();

            boolean inactive = "INACTIVE".equalsIgnoreCase(scholasticStatus);

            if (inactive) {
                return new ActivationResult(
                        false,
                        "Student is not eligible for activation. Please contact the registrar.",
                        null
                );
            }

            return createActivatedAccount(
                    originalSchoolId,
                    normalizedUsername,
                    student.getEmailAddress(),
                    "STUDENT",
                    ipAddress
            );
        }

        Optional<FacultyProfileCache> facultyOpt =
                facultyProfileCacheRepository.findByEmployeeIdAndEmailAddressAndBirthDate(
                        originalSchoolId,
                        cleanedEmail,
                        cleanedBirthday
                );

        if (facultyOpt.isPresent()) {
            FacultyProfileCache faculty = facultyOpt.get();

            if (!"ACTIVE".equalsIgnoreCase(faculty.getStatus())) {
                return new ActivationResult(
                        false,
                        "Faculty is not eligible for activation. Please contact the registrar.",
                        null
                );
            }

            return createActivatedAccount(
                    originalSchoolId,
                    normalizedUsername,
                    faculty.getEmailAddress(),
                    "FACULTY",
                    ipAddress
            );
        }

        return new ActivationResult(false, "No matching record was found.", null);
    }

    @TrackActivity(
            module = "AUTH",
            action = "CREATE_ACCOUNT",
            message = "User account created successfully"
    )
    @Transactional
    public ActivationResult createActivatedAccount(

            @ActivityTarget(ActivityTargetType.TARGET_USER_ID)
            String originalSchoolId,

            String normalizedUsername,
            String email,

            @ActivityTarget(ActivityTargetType.TARGET_ROLE)
            String role,

            String ipAddress
    ) {
        try {
            String tempPassword = generateTempPassword();

            UserAccess user = new UserAccess();
            user.setSchoolId(originalSchoolId);
            user.setUsername(normalizedUsername);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(tempPassword));
            user.setRole(role);
            user.setActive(true);
            user.setBlocked(false);
            user.setMustChangePassword(true);
            user.setFailedLoginAttempts(0);
            user.setActivatedAt(TimeUtil.now());
            user.setTempPasswordSentAt(TimeUtil.now());
            user.setTempPasswordExpiry(TimeUtil.now().plusMinutes(5));

            userAccessRepository.save(user);

            System.out.println("EMAIL DEBUG: before sending to " + email);

            emailService.sendActivationEmail(email, normalizedUsername, tempPassword);

            System.out.println("EMAIL DEBUG: after sending to " + email);

            logEvent(user, originalSchoolId, normalizedUsername,
                    "ACTIVATE_ACCOUNT", "SUCCESS", role + " account activated successfully", ipAddress);

            logEvent(user, originalSchoolId, normalizedUsername,
                    "TEMP_PASSWORD_SENT", "SUCCESS", "Temporary password sent to email", ipAddress);

            return new ActivationResult(
                    true,
                    "Account activated successfully. Temporary credentials have been sent to your email.",
                    role
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to activate account. Root cause: " + e.getMessage(), e);
        }
    }

    @TrackActivity(
            module = "AUTH",
            action = "LOGIN",
            message = "User login attempt"
    )
    public LoginResult login(

            @ActivityTarget(ActivityTargetType.TARGET_USER_ID)
            String schoolIdInput,

            String password,
            String ipAddress
    ) {
        String typedValue = sanitizeSchoolId(schoolIdInput);
        String normalizedUsername = normalizeId(typedValue);

        Optional<UserAccess> userOpt = userAccessRepository.findByUsername(normalizedUsername);

        if (userOpt.isEmpty()) {
            return new LoginResult(false, "Account not found.");
        }

        UserAccess user = userOpt.get();

        if (!user.isActive()) {
            String msg = "Inactive account.";
            logEvent(user, user.getSchoolId(), user.getUsername(), "LOGIN", "FAILED", msg, ipAddress);
            return new LoginResult(false, msg);
        }

        if (user.isBlocked()) {
            String msg = "Account is blocked. Please use Forgot Password.";
            logEvent(user, user.getSchoolId(), user.getUsername(), "LOGIN", "BLOCKED", msg, ipAddress);
            return new LoginResult(false, msg);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 3) {
                user.setBlocked(true);
                user.setBlockedAt(TimeUtil.now());
                userAccessRepository.save(user);

                String msg = "Account blocked after 3 failed attempts.";
                logEvent(user, user.getSchoolId(), user.getUsername(), "ACCOUNT_BLOCKED", "BLOCKED", msg, ipAddress);
                accountStatusLogService.log(
                        user,
                        "BLOCKED",
                        msg,
                        user.getUsername(),
                        "ACTIVE",
                        "BLOCKED"
                );
                return new LoginResult(false, msg);
            }

            userAccessRepository.save(user);

            String msg = "Invalid password. Attempt " + attempts + " of 3.";
            logEvent(user, user.getSchoolId(), user.getUsername(), "LOGIN", "FAILED", msg, ipAddress);
            return new LoginResult(false, msg);
        }

        if (user.isMustChangePassword()
                && user.getTempPasswordExpiry() != null
                && TimeUtil.now().isAfter(user.getTempPasswordExpiry())) {
            String msg = "Temporary password expired. Please request a new one.";
            logEvent(user, user.getSchoolId(), user.getUsername(), "LOGIN", "FAILED", msg, ipAddress);
            return new LoginResult(false, msg);
        }

        revokeExistingActiveSession(user, ipAddress);


        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(TimeUtil.now());
        userAccessRepository.save(user);

        String msg = "Login successful.";
        logEvent(user, user.getSchoolId(), user.getUsername(), "LOGIN", "SUCCESS", msg, ipAddress);

        String sessionToken = createSession(user, "Session started successfully", ipAddress);

        String firstName = null;
        String lastName = null;
        String fullName = null;
        String emailAddress = null;

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {

            AdminProfile admin =
                    adminProfileRepository
                            .findByEmployeeId(user.getSchoolId())
                            .orElse(null);

            if (admin != null) {
                firstName = admin.getFirstName();
                lastName = admin.getLastName();
                emailAddress = admin.getEmail();
            }
        }

        else if ("FACULTY".equalsIgnoreCase(user.getRole())) {

            FacultyProfileCache faculty =
                    facultyProfileCacheRepository
                            .findByEmployeeId(user.getSchoolId())
                            .orElse(null);

            if (faculty != null) {
                firstName = faculty.getFirstName();
                lastName = faculty.getLastName();
                emailAddress = faculty.getEmailAddress();
            }
        }

        else if ("STUDENT".equalsIgnoreCase(user.getRole())) {

            StudentProfileCache student =
                    studentProfileCacheRepository
                            .findByStudentId(user.getSchoolId())
                            .orElse(null);

            if (student != null) {
                firstName = student.getFirstName();
                lastName = student.getLastName();
                emailAddress = student.getEmailAddress();
            }
        }

        fullName = (
                (firstName == null ? "" : firstName) + " " +
                        (lastName == null ? "" : lastName)
        ).trim();

        return new LoginResult(
                true,
                "Login successful.",
                user.getUsername(),
                user.getSchoolId(),
                user.getRole(),
                user.isMustChangePassword(),
                sessionToken,
                firstName,
                lastName,
                emailAddress
        );
    }

    @TrackActivity(
            module = "AUTH",
            action = "CHANGE_PASSWORD",
            message = "User password change attempt"
    )
    public boolean changePassword(
            ChangePasswordRequest request,
            String ipAddress
    ) {
        String typedValue = sanitizeSchoolId(request.getSchoolId());
        String normalizedUsername = normalizeId(typedValue);

        Optional<UserAccess> userOpt = userAccessRepository.findByUsername(normalizedUsername);

        if (userOpt.isEmpty()) {
            logEvent(null, typedValue, normalizedUsername,
                    "CHANGE_PASSWORD", "FAILED", "Account not found", ipAddress);
            return false;
        }

        UserAccess user = userOpt.get();

        if (!user.isActive()) {
            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "CHANGE_PASSWORD", "FAILED", "Inactive account", ipAddress);
            return false;
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "CHANGE_PASSWORD", "FAILED", "Current password is incorrect", ipAddress);
            return false;
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "CHANGE_PASSWORD", "FAILED", "New password is empty", ipAddress);
            return false;
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "CHANGE_PASSWORD", "FAILED", "New password and confirm password do not match", ipAddress);
            return false;
        }

        if (request.getNewPassword().length() < 8) {
            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "CHANGE_PASSWORD", "FAILED", "New password must be at least 8 characters", ipAddress);
            return false;
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "CHANGE_PASSWORD", "FAILED", "New password must be different from current password", ipAddress);
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setBlocked(false);
        user.setFailedLoginAttempts(0);
        user.setTempPasswordExpiry(null);

        userAccessRepository.save(user);

        logEvent(user, user.getSchoolId(), user.getUsername(),
                "CHANGE_PASSWORD", "SUCCESS", "Password changed successfully", ipAddress);

        return true;
    }

    @TrackActivity(
            module = "AUTH",
            action = "FORGOT_PASSWORD",
            message = "User forgot password request"
    )
    public boolean forgotPassword(
            ForgotPasswordRequest request,
            String ipAddress
    ) {
        String originalSchoolId = sanitizeSchoolId(request.getSchoolId());
        String cleanedEmail = request.getEmail() == null ? "" : request.getEmail().trim();
        String cleanedBirthday = normalizeDateString(request.getBirthday());
        String normalizedUsername = normalizeId(originalSchoolId);

        boolean adminValid = false;

        Optional<AdminProfile> adminOpt = adminProfileRepository.findByEmployeeId(originalSchoolId);
        if (adminOpt.isPresent()) {
            AdminProfile admin = adminOpt.get();

            boolean emailMatch = admin.getEmail() != null
                    && admin.getEmail().trim().equalsIgnoreCase(cleanedEmail);

            boolean birthDateMatch = admin.getBirthDate() != null
                    && admin.getBirthDate().toString().equals(cleanedBirthday);

            boolean profileActive = Boolean.TRUE.equals(admin.getIsActive());

            adminValid = emailMatch && birthDateMatch && profileActive;
        }

        Optional<UserAccess> userOpt = userAccessRepository.findByUsername(normalizedUsername);

        if (userOpt.isEmpty()) {
            logEvent(null, originalSchoolId, normalizedUsername,
                    "FORGOT_PASSWORD", "FAILED", "Account not found or not yet activated", ipAddress);
            return false;
        }

        UserAccess user = userOpt.get();

        if (!user.isActive()) {
            logEvent(null, originalSchoolId, normalizedUsername,
                    "FORGOT_PASSWORD", "FAILED", "Account not active. Please contact admin.", ipAddress);
            return false;
        }

        boolean studentValid =
                studentProfileCacheRepository
                        .findByStudentIdAndEmailAddressAndBirthDate(
                                originalSchoolId,
                                cleanedEmail,
                                cleanedBirthday
                        )
                        .filter(s -> !"INACTIVE".equalsIgnoreCase(s.getScholasticStatus()))
                        .isPresent();

        boolean facultyValid =
                facultyProfileCacheRepository
                        .findByEmployeeIdAndEmailAddressAndBirthDate(
                                originalSchoolId,
                                cleanedEmail,
                                cleanedBirthday
                        )
                        .filter(f -> "ACTIVE".equalsIgnoreCase(f.getStatus()))
                        .isPresent();

        if (!studentValid && !facultyValid && !adminValid) {
            logEvent(user, originalSchoolId, normalizedUsername,
                    "FORGOT_PASSWORD", "FAILED", "Identity verification failed", ipAddress);
            return false;
        }

        String previousStatus = user.isBlocked() ? "BLOCKED" : "ACTIVE";

        String tempPassword = generateTempPassword();
        String email = resolveUserEmail(user);

        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setBlocked(false);
        user.setFailedLoginAttempts(0);
        user.setMustChangePassword(true);
        user.setBlockedAt(null);
        user.setTempPasswordSentAt(TimeUtil.now());
        user.setTempPasswordExpiry(TimeUtil.now().plusMinutes(5));

        userAccessRepository.save(user);

        if ("BLOCKED".equals(previousStatus)) {
            accountStatusLogService.log(
                    user,
                    "UNBLOCKED",
                    "Account unblocked after successful forgot password verification",
                    user.getUsername(),
                    "BLOCKED",
                    "ACTIVE"
            );
        }

        emailService.sendResetPasswordEmail(email, normalizedUsername, tempPassword);

        logEvent(user, originalSchoolId, normalizedUsername,
                "FORGOT_PASSWORD", "SUCCESS", "Forgot password verified successfully", ipAddress);

        logEvent(user, originalSchoolId, normalizedUsername,
                "TEMP_PASSWORD_SENT", "SUCCESS", "New temporary password sent to email", ipAddress);

        return true;
    }

    @TrackActivity(
            module = "AUTH",
            action = "LOGOUT",
            message = "User logout request"
    )
    public boolean logout(
            LogoutRequest request,
            String ipAddress
    ) {
        if (request.getSessionToken() == null || request.getSessionToken().isBlank()) {
            return false;
        }

        Optional<UserSessionLog> sessionOpt = userSessionLogRepository.findBySessionTokenWithUser(request.getSessionToken());

        if (sessionOpt.isEmpty()) {
            return false;
        }

        UserSessionLog session = sessionOpt.get();
        session.setLoginStatus("LOGGED_OUT");
        session.setLogoutAt(TimeUtil.now());
        session.setMessage("User logged out successfully");

        userSessionLogRepository.save(session);

        UserAccess user = session.getUserAccess();

        if (user != null) {
            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "LOGOUT", "SUCCESS", "User logged out successfully", ipAddress);
        }

        return true;
    }

    public boolean isSessionValid(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }

        Optional<UserSessionLog> sessionOpt = userSessionLogRepository.findBySessionTokenWithUser(sessionToken);

        if (sessionOpt.isEmpty()) {
            return false;
        }

        UserSessionLog session = sessionOpt.get();
        return "ACTIVE".equals(session.getLoginStatus());
    }

    private void revokeExistingActiveSession(UserAccess user, String ipAddress) {
        Optional<UserSessionLog> activeSessionOpt =
                userSessionLogRepository.findFirstByUserAccess_AccessIdAndLoginStatusOrderByLoginAtDesc(
                        user.getAccessId(),
                        "ACTIVE"
                );

        if (activeSessionOpt.isPresent()) {
            UserSessionLog oldSession = activeSessionOpt.get();
            oldSession.setLoginStatus("REVOKED");
            oldSession.setLogoutAt(TimeUtil.now());
            oldSession.setMessage("Previous active session revoked due to new login");

            userSessionLogRepository.save(oldSession);

            logEvent(user, user.getSchoolId(), user.getUsername(),
                    "SESSION_REVOKED", "SUCCESS", "Previous active session revoked due to new login", ipAddress);
        }
    }

    private String createSession(UserAccess user, String message, String ipAddress) {
        String sessionToken = generateSessionToken();

        UserSessionLog sessionLog = new UserSessionLog();
        sessionLog.setUserAccess(user);
        sessionLog.setSchoolId(user.getSchoolId());
        sessionLog.setUsername(user.getUsername());
        sessionLog.setSessionToken(sessionToken);
        sessionLog.setLoginStatus("ACTIVE");
        sessionLog.setMessage(message);
        sessionLog.setLoginAt(TimeUtil.now());
        sessionLog.setExpiresAt(TimeUtil.now().plusHours(8));
        sessionLog.setIpAddress(ipAddress);

        userSessionLogRepository.save(sessionLog);

        return sessionToken;
    }

    @Transactional(readOnly = true)
    public UserAccess getUserFromSession(String sessionToken) {

        if (sessionToken == null || sessionToken.isBlank()) {
            return null;
        }

        String cleanedToken = sessionToken
                .replace("Bearer ", "")
                .trim();

        Optional<UserSessionLog> sessionOpt =
                userSessionLogRepository.findBySessionTokenWithUser(cleanedToken);

        if (sessionOpt.isEmpty()) {
            return null;
        }

        UserSessionLog session = sessionOpt.get();

        if (!"ACTIVE".equalsIgnoreCase(session.getLoginStatus())) {
            return null;
        }

        if (session.getLogoutAt() != null) {
            return null;
        }

        if (session.getExpiresAt() == null || TimeUtil.now().isAfter(session.getExpiresAt())) {
            session.setLoginStatus("EXPIRED");
            session.setMessage("Session expired.");
            userSessionLogRepository.save(session);
            return null;
        }

        UserAccess user = session.getUserAccess();

        if (user == null || !user.isActive() || user.isBlocked()) {
            return null;
        }

        return user;
    }

    private void logEvent(UserAccess user,
                          String schoolId,
                          String username,
                          String eventType,
                          String eventStatus,
                          String message,
                          String ipAddress) {
        UserAccessLog log = new UserAccessLog();
        log.setUserAccess(user);
        log.setSchoolId(schoolId);
        log.setUsername(username);
        log.setEventType(eventType);
        log.setEventStatus(eventStatus);
        log.setMessage(message);
        log.setIpAddress(ipAddress);

        userAccessLogRepository.save(log);
    }

    private String sanitizeSchoolId(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private String normalizeDateString(String value) {
        if (value == null) return "";
        String cleaned = value.trim();

        if (cleaned.length() >= 10) {
            return cleaned.substring(0, 10);
        }

        return cleaned;
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    private String generateSessionToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String resolveUserEmail(UserAccess user) {
        try {
            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                Optional<StudentProfileCache> studentOpt =
                        studentProfileCacheRepository.findById(user.getSchoolId());

                if (studentOpt.isPresent() && studentOpt.get().getEmailAddress() != null) {
                    return studentOpt.get().getEmailAddress();
                }

            } else if ("FACULTY".equalsIgnoreCase(user.getRole())) {
                Optional<FacultyProfileCache> facultyOpt =
                        facultyProfileCacheRepository.findById(user.getSchoolId());

                if (facultyOpt.isPresent() && facultyOpt.get().getEmailAddress() != null) {
                    return facultyOpt.get().getEmailAddress();
                }

            } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                Optional<AdminProfile> adminOpt =
                        adminProfileRepository.findByEmployeeId(user.getSchoolId());

                if (adminOpt.isPresent() && adminOpt.get().getEmail() != null) {
                    return adminOpt.get().getEmail();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return user.getEmail();
    }
}