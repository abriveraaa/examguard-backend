package com.example.backend.service.core;

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
import com.example.backend.repository.cache.ClassOfferingCacheRepository;
import com.example.backend.repository.cache.FacultyLoadCacheRepository;
import com.example.backend.repository.cache.FacultyProfileCacheRepository;
import com.example.backend.repository.cache.ClassEnrollmentCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.repository.core.UserAccessLogRepository;
import com.example.backend.repository.core.UserAccessRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminUserManagementService {

    private final UserAccessRepository userAccessRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final UserAccessLogRepository userAccessLogRepository;

    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final ClassEnrollmentCacheRepository classEnrollmentCacheRepository;
    private final FacultyLoadCacheRepository facultyLoadCacheRepository;
    private final ClassOfferingCacheRepository classOfferingCacheRepository;

    public AdminUserManagementService(UserAccessRepository userAccessRepository,
                                      AdminProfileRepository adminProfileRepository,
                                      UserAccessLogRepository userAccessLogRepository,
                                      StudentProfileCacheRepository studentProfileCacheRepository,
                                      FacultyProfileCacheRepository facultyProfileCacheRepository,
                                      ClassEnrollmentCacheRepository classEnrollmentCacheRepository,
                                      FacultyLoadCacheRepository facultyLoadCacheRepository,
                                      ClassOfferingCacheRepository classOfferingCacheRepository) {
        this.userAccessRepository = userAccessRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.userAccessLogRepository = userAccessLogRepository;
        this.studentProfileCacheRepository = studentProfileCacheRepository;
        this.facultyProfileCacheRepository = facultyProfileCacheRepository;
        this.classEnrollmentCacheRepository = classEnrollmentCacheRepository;
        this.facultyLoadCacheRepository = facultyLoadCacheRepository;
        this.classOfferingCacheRepository = classOfferingCacheRepository;
    }

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

    public UserDetailsResponse getUserDetails(String schoolId, String role) {
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

    private record SectionSnapshot(
            String programCode,
            String yearLevel,
            String sectionCode,
            String sectionName
    ) {}
}