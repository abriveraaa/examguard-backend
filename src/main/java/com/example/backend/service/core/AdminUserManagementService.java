package com.example.backend.service.core;

import com.example.backend.audit.ActivityTarget;
import com.example.backend.audit.ActivityTargetType;
import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.admin.users.AdminUserRowDto;
import com.example.backend.dto.admin.users.AdminUsersExportRequest;
import com.example.backend.dto.core.AdminUserResponse;
import com.example.backend.dto.core.FacultyUserResponse;
import com.example.backend.dto.student.dashboard.StudentUserResponse;
import com.example.backend.dto.core.UserDetailsResponse;
import com.example.backend.entity.cache.ClassOfferingCache;
import com.example.backend.entity.cache.FacultyProfileCache;
import com.example.backend.entity.cache.ClassEnrollmentCache;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.AdminProfile;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.entity.core.UserAccessLog;
import com.example.backend.report.admin.AdminUsersExcelExporter;
import com.example.backend.report.admin.AdminUsersPdfExporter;
import com.example.backend.repository.cache.ClassOfferingCacheRepository;
import com.example.backend.repository.cache.FacultyLoadCacheRepository;
import com.example.backend.repository.cache.FacultyProfileCacheRepository;
import com.example.backend.repository.cache.ClassEnrollmentCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.repository.core.UserAccessLogRepository;
import com.example.backend.repository.core.UserAccessRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AdminUserManagementService {

    private final UserAccessRepository userAccessRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final UserAccessLogRepository userAccessLogRepository;
    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final ClassEnrollmentCacheRepository classEnrollmentCacheRepository;
    private final FacultyLoadCacheRepository facultyLoadCacheRepository;
    private final ClassOfferingCacheRepository classOfferingCacheRepository;
    private final AdminUsersPdfExporter adminUsersPdfExporter;
    private final AdminUsersExcelExporter adminUsersExcelExporter;

    @TrackActivity(
            module = "ADMIN_USERS",
            action = "EXPORT_USERS",
            message = "Admin exported user management report"
    )
    public byte[] exportUsers(AdminUsersExportRequest request, UserAccess currentAdmin) {

        List<AdminUserRowDto> rows = getUsersForExport(request);

        String format
                = request.getFormat() == null ? "PDF" : request.getFormat().trim().toUpperCase();

        if ("EXCEL".equals(format) || "XLSX".equals(format)) {
            return adminUsersExcelExporter.export(rows, request, currentAdmin);
        }

        return adminUsersPdfExporter.export(rows, request, currentAdmin);
    }

    @TrackActivity(
            module = "USER_MANAGEMENT",
            action = "VIEW_ADMINS",
            message = "Admin viewed admin users"
    )
    public List<AdminUserResponse> getAdmins() {
        List<AdminUserResponse> result = new ArrayList<>();

        for (AdminProfile admin : adminProfileRepository.findAll()) {
            Optional<UserAccess> userOpt = findUserAccess(admin.getEmployeeId());

            result.add(new AdminUserResponse(
                    admin.getEmployeeId(),
                    userOpt.map(UserAccess::getUsername).orElse("-"),
                    admin.getFirstName(),
                    admin.getLastName(),
                    safe(admin.getEmail()),
                    admin.getBirthDate(),
                    Boolean.TRUE.equals(admin.getIsActive()) ? "Active" : "Inactive",
                    resolveSystemAccess(userOpt)
            ));
        }

        return result;
    }

    @TrackActivity(
            module = "USER_MANAGEMENT",
            action = "VIEW_STUDENTS",
            message = "Admin viewed student users"
    )
    public List<StudentUserResponse> getStudents() {
        List<StudentUserResponse> result = new ArrayList<>();

        for (StudentProfileCache student : studentProfileCacheRepository.findAll()) {
            Optional<UserAccess> userOpt = findUserAccess(student.getStudentId());

            result.add(new StudentUserResponse(
                    student.getStudentId(),
                    userOpt.map(UserAccess::getUsername).orElse("-"),
                    student.getFirstName(),
                    student.getLastName(),
                    safe(student.getEmailAddress()),
                    student.getCollegeName(),
                    student.getProgramName(),
                    student.getYearLevel(),
                    student.getSectionName(),
                    student.getScholasticStatus(),
                    resolveSystemAccess(userOpt)
            ));
        }

        return result;
    }

    @TrackActivity(
            module = "USER_MANAGEMENT",
            action = "VIEW_FACULTY",
            message = "Admin viewed faculty users"
    )
    public List<FacultyUserResponse> getFaculty() {
        List<FacultyUserResponse> result = new ArrayList<>();

        for (FacultyProfileCache faculty : facultyProfileCacheRepository.findAll()) {
            Optional<UserAccess> userOpt = findUserAccess(faculty.getEmployeeId());

            int activeLoads = facultyLoadCacheRepository
                    .findByEmployeeId(faculty.getEmployeeId())
                    .size();

            result.add(new FacultyUserResponse(
                    faculty.getEmployeeId(),
                    userOpt.map(UserAccess::getUsername).orElse("-"),
                    faculty.getFirstName(),
                    faculty.getLastName(),
                    safe(faculty.getEmailAddress()),
                    safeStatus(faculty.getStatus()),
                    resolveSystemAccess(userOpt),
                    activeLoads + " active load(s)"
            ));
        }

        return result;
    }

    @TrackActivity(
            module = "USER_MANAGEMENT",
            action = "VIEW_USER_DETAILS",
            message = "Admin viewed user details"
    )
    public UserDetailsResponse getUserDetails(
            @ActivityTarget(ActivityTargetType.TARGET_USER_ID)
            String schoolId,

            @ActivityTarget(ActivityTargetType.TARGET_ROLE)
            String role
    ) {
        String safeRole = safe(role).toUpperCase();
        String safeSchoolId = safe(schoolId);

        Optional<UserAccess> userOpt = findUserAccess(safeSchoolId);

        String username = userOpt.map(UserAccess::getUsername).orElse("-");
        Integer failedAttempts = userOpt.map(UserAccess::getFailedLoginAttempts).orElse(0);
        String systemAccess = resolveSystemAccess(userOpt);
        String lastLogin = getLatestLogin(userOpt);

        if ("ADMIN".equals(safeRole)) {
            Optional<AdminProfile> adminOpt = adminProfileRepository.findByEmployeeId(safeSchoolId);
            if (adminOpt.isEmpty()) return null;

            AdminProfile admin = adminOpt.get();

            return new UserDetailsResponse(
                    admin.getEmployeeId(),
                    username,
                    buildFullName(admin.getFirstName(), admin.getLastName()),
                    safe(admin.getEmail()),
                    "ADMIN",
                    null,
                    null,
                    null,
                    null,
                    Boolean.TRUE.equals(admin.getIsActive()) ? "Active" : "Inactive",
                    systemAccess,
                    failedAttempts,
                    lastLogin
            );
        }

        if ("STUDENT".equals(safeRole)) {
            Optional<StudentProfileCache> studentOpt =
                    studentProfileCacheRepository.findById(safeSchoolId);

            if (studentOpt.isEmpty()) return null;

            StudentProfileCache student = studentOpt.get();
            SectionSnapshot section = getStudentPrimarySection(student.getStudentId());

            return new UserDetailsResponse(
                    safe(student.getStudentId()),
                    username,
                    buildFullName(student.getFirstName(), student.getLastName()),
                    safe(student.getEmailAddress()),
                    "STUDENT",
                    student.getCollegeName(),
                    student.getProgramName(),
                    student.getYearLevel(),
                    student.getSectionName(),
                    student.getScholasticStatus(),
                    systemAccess,
                    failedAttempts,
                    lastLogin
            );
        }

        if ("FACULTY".equals(safeRole)) {
            Optional<FacultyProfileCache> facultyOpt =
                    facultyProfileCacheRepository.findById(safeSchoolId);

            if (facultyOpt.isEmpty()) return null;

            FacultyProfileCache faculty = facultyOpt.get();

            int activeLoads = facultyLoadCacheRepository
                    .findByEmployeeId(faculty.getEmployeeId())
                    .size();

            return new UserDetailsResponse(
                    safe(faculty.getEmployeeId()),
                    username,
                    buildFullName(faculty.getFirstName(), faculty.getLastName()),
                    safe(faculty.getEmailAddress()),
                    "FACULTY",
                    null,
                    null,
                    null,
                    null,
                    faculty.getStatus() + "," + activeLoads + " active load(s)",
                    systemAccess,
                    failedAttempts,
                    lastLogin
            );
        }

        return null;
    }

    // ====================
    // HELPERS
    // ===================

    private List<AdminUserRowDto> getUsersForExport(AdminUsersExportRequest request) {

        String role = request.getRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return exportAdmins(request);
        }

        if ("STUDENT".equalsIgnoreCase(role)) {
            return exportStudents(request);
        }

        if ("FACULTY".equalsIgnoreCase(role)) {
            return exportFaculty(request);
        }

        List<AdminUserRowDto> rows = new ArrayList<>();

        rows.addAll(exportAdmins(request));
        rows.addAll(exportStudents(request));
        rows.addAll(exportFaculty(request));

        return rows;
    }

    private List<AdminUserRowDto> exportStudents(AdminUsersExportRequest request) {

        return studentProfileCacheRepository.findAll()
                .stream()
                .map(student -> {
                    UserAccess access = findAccess(student.getStudentId());

                    return new AdminUserRowDto(
                            student.getStudentId(),
                            access == null ? "-" : safe(access.getUsername()),
                            buildFullName(student.getFirstName(), student.getLastName()),
                            safe(student.getEmailAddress()),
                            safe(student.getCollegeCode().toUpperCase()),
                            safe(student.getProgramCode().toUpperCase()),
                            student.getYearLevel(),
                            safe(student.getScholasticStatus()),
                            resolveSystemAccess(access)
                    );
                })
                .filter(row -> matchesKeyword(row, request.getKeyword()))
                .filter(row -> matchesRegistrarStatus(row, request.getStatus()))
                .filter(row -> matchesReactivation(row, request.getReactivation()))
                .toList();
    }

    private List<AdminUserRowDto> exportAdmins(AdminUsersExportRequest request) {

        return adminProfileRepository.findAll()
                .stream()
                .map(admin -> {
                    UserAccess access = findAccess(admin.getEmployeeId());

                    return new AdminUserRowDto(
                            admin.getEmployeeId(),
                            access == null ? "-" : safe(access.getUsername()),
                            buildFullName(admin.getFirstName(), admin.getLastName()),
                            safe(admin.getEmail()),
                            Boolean.TRUE.equals(admin.getIsActive()) ? "Active" : "Inactive",
                            resolveSystemAccess(access)
                    );
                })
                .filter(row -> matchesKeyword(row, request.getKeyword()))
                .filter(row -> matchesRegistrarStatus(row, request.getStatus()))
                .toList();
    }

    private List<AdminUserRowDto> exportFaculty(AdminUsersExportRequest request) {

        return facultyProfileCacheRepository.findAll()
                .stream()
                .map(faculty -> {
                    UserAccess access = findAccess(faculty.getEmployeeId());

                    return new AdminUserRowDto(
                            faculty.getEmployeeId(),
                            access == null ? "-" : safe(access.getUsername()),
                            buildFullName(faculty.getFirstName(), faculty.getLastName()),
                            safe(faculty.getEmailAddress()),
                            safe(faculty.getStatus()),
                            resolveSystemAccess(access)
                    );
                })
                .filter(row -> matchesKeyword(row, request.getKeyword()))
                .filter(row -> matchesRegistrarStatus(row, request.getStatus()))
                .filter(row -> matchesReactivation(row, request.getReactivation()))
                .toList();
    }

    private SectionSnapshot getStudentPrimarySection(String studentId) {
        List<ClassEnrollmentCache> enrollments =
                classEnrollmentCacheRepository.findByStudentId(studentId);

        if (enrollments.isEmpty()) {
            return new SectionSnapshot("-", "-", "-", "-");
        }

        String classOfferingId = enrollments.get(0).getClassOfferingId();

        Optional<ClassOfferingCache> offeringOpt =
                classOfferingCacheRepository.findById(classOfferingId);

        if (offeringOpt.isEmpty()) {
            return new SectionSnapshot("-", "-", "-", "-");
        }

        ClassOfferingCache offering = offeringOpt.get();

        String programCode = safe(offering.getProgramCode());
        String yearLevel = offering.getYearLevel() == null ? "-" : String.valueOf(offering.getYearLevel());
        String sectionCode = safe(offering.getSectionName());
        String sectionName = programCode + " " + yearLevel + "-" + sectionCode;

        return new SectionSnapshot(programCode, yearLevel, sectionCode, sectionName);
    }

    private Optional<UserAccess> findUserAccess(String schoolId) {
        return userAccessRepository.findBySchoolId(safe(schoolId));
    }

    private String getLatestLogin(Optional<UserAccess> userOpt) {
        if (userOpt.isEmpty()) {
            return null;
        }

        String schoolId = userOpt.get().getSchoolId();

        Optional<UserAccessLog> latestLoginOpt =
                userAccessLogRepository.findFirstBySchoolIdAndEventTypeAndEventStatusOrderByCreatedAtDesc(
                        schoolId,
                        "LOGIN",
                        "SUCCESS"
                );

        return latestLoginOpt
                .map(UserAccessLog::getCreatedAt)
                .map(Object::toString)
                .orElse(null);
    }

    private String resolveSystemAccess(Optional<UserAccess> userOpt) {
        if (userOpt.isEmpty()) {
            return "Not Activated";
        }

        UserAccess user = userOpt.get();

        if (user.isBlocked()) {
            return "Blocked";
        }

        if (!user.isActive()) {
            return "Suspended";
        }

        return "Active";
    }

    private UserAccess findAccess(String schoolId) {
        if (schoolId == null || schoolId.isBlank()) {
            return null;
        }

        return userAccessRepository.findBySchoolId(schoolId)
                .orElse(null);
    }

    private String resolveSystemAccess(UserAccess access) {

        if (access == null) {
            return "No Access";
        }

        if (access.isBlocked()) {
            return "Blocked";
        }

        if (!access.isActive()) {
            return "Inactive";
        }

        return "Active";
    }

    private boolean matchesKeyword(AdminUserRowDto row, String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String key = keyword.toLowerCase();

        return contains(row.getSchoolId(), key)
                || contains(row.getUsername(), key)
                || contains(row.getFullName(), key)
                || contains(row.getEmail(), key)
                || contains(row.getRegistrarStatus(), key)
                || contains(row.getSystemAccess(), key);
    }

    private boolean matchesRegistrarStatus(AdminUserRowDto row, String status) {

        if (status == null
                || status.isBlank()
                || status.equalsIgnoreCase("All")) {
            return true;
        }

        return status.equalsIgnoreCase(row.getRegistrarStatus());
    }

    private boolean matchesReactivation(AdminUserRowDto row, String reactivation) {

        if (reactivation == null
                || reactivation.isBlank()
                || reactivation.equalsIgnoreCase("All")) {
            return true;
        }

        if (!reactivation.equalsIgnoreCase("Pending Reactivation")) {
            return true;
        }

        UserAccess access = findAccess(row.getSchoolId());

        return access != null && Boolean.TRUE.equals(access.getEligibleForReactivation());
    }

    private boolean contains(String value, String key) {
        return value != null && value.toLowerCase().contains(key);
    }

    private String buildFullName(String firstName, String lastName) {
        String full = (safe(firstName) + " " + safe(lastName)).trim();
        return full.isBlank() ? "-" : full;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String safeStatus(String value) {
        return value == null || value.isBlank() ? "Inactive" : value.trim();
    }

    private String resolveStatus(UserAccess access) {

        if (access.isBlocked()) {
            return "BLOCKED";
        }

        if (!access.isActive()) {
            return "INACTIVE";
        }

        return "ACTIVE";
    }

    private boolean matchesStatus(UserAccess access, String status) {

        if (status == null
                || status.isBlank()
                || status.equalsIgnoreCase("All Statuses")) {
            return true;
        }

        String actualStatus = resolveStatus(access);

        return status.equalsIgnoreCase(actualStatus);
    }

    private boolean matchesReactivation(
            UserAccess access,
            String reactivation
    ) {

        if (reactivation == null
                || reactivation.isBlank()
                || reactivation.equalsIgnoreCase("All")) {
            return true;
        }

        if (!reactivation.equalsIgnoreCase("Pending Reactivation")) {
            return true;
        }

        return Boolean.TRUE.equals(access.getEligibleForReactivation());
    }

    private boolean matchesRole(UserAccess user, String role) {
        if (role == null || role.isBlank() || role.equalsIgnoreCase("All Roles")) {
            return true;
        }

        return role.equalsIgnoreCase(user.getRole());
    }

    private String formatDate(OffsetDateTime value) {

        if (value == null) {
            return "-";
        }

        return value
                .atZoneSameInstant(ZoneId.of("Asia/Manila"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private boolean matchesKeyword(UserAccess user, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String q = search.toLowerCase();

        return safe(user.getSchoolId()).toLowerCase().contains(q)
                || safe(user.getUsername()).toLowerCase().contains(q)
                || safe(user.getEmail()).toLowerCase().contains(q)
                || safe(user.getRole()).toLowerCase().contains(q);
    }

    private record SectionSnapshot(
            String programCode,
            String yearLevel,
            String sectionCode,
            String sectionName
    ) {}
}