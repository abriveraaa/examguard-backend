package com.example.backend.service.registrar;

import com.example.backend.dto.registrar.*;
import com.example.backend.entity.cache.*;
import com.example.backend.entity.core.RegistrarSyncLog;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.cache.*;
import com.example.backend.repository.core.RegistrarSyncLogRepository;
import com.example.backend.utility.TimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class RegistrarSyncService {

    private final RegistrarApiService api;

    private final RegistrarSyncLogRepository registrarSyncLogRepository;
    private final StudentProfileCacheRepository studentRepo;
    private final FacultyProfileCacheRepository facultyRepo;
    private final ClassOfferingCacheRepository offeringRepo;
    private final ClassEnrollmentCacheRepository enrollmentRepo;
    private final FacultyLoadCacheRepository facultyLoadRepo;

    public RegistrarSyncService(
            RegistrarApiService api,
            RegistrarSyncLogRepository registrarSyncLogRepository,
            StudentProfileCacheRepository studentRepo,
            FacultyProfileCacheRepository facultyRepo,
            ClassOfferingCacheRepository offeringRepo,
            ClassEnrollmentCacheRepository enrollmentRepo,
            FacultyLoadCacheRepository facultyLoadRepo
    ) {
        this.api = api;
        this.registrarSyncLogRepository = registrarSyncLogRepository;
        this.studentRepo = studentRepo;
        this.facultyRepo = facultyRepo;
        this.offeringRepo = offeringRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.facultyLoadRepo = facultyLoadRepo;
    }

    @Transactional
    public String initialSync(UserAccess performedBy) {

        String actor = (performedBy != null && performedBy.getUsername() != null)
                ? performedBy.getUsername()
                : "SYSTEM";

        RegistrarSyncLog log = new RegistrarSyncLog();
        log.setSyncType("INITIAL_SYNC");
        log.setStatus("STARTED");
        log.setStartedAt(TimeUtil.now());
        log.setPerformedBy(actor);
        log.setRecordsAffected(0);

        registrarSyncLogRepository.save(log);

        try {
            syncStudents();
            syncFaculty();
            syncClassOfferings();
            syncClassEnrollments();
            syncFacultyLoads();

            log.setStatus("SUCCESS");
            log.setFinishedAt(TimeUtil.now());
            log.setMessage("Registrar sync completed successfully.");

            registrarSyncLogRepository.save(log);

            return "Registrar sync completed successfully.";

        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setFinishedAt(TimeUtil.now());
            log.setMessage(e.getMessage());

            registrarSyncLogRepository.save(log);

            throw e;
        }
    }

    private void syncStudents() {
        List<StudentDTO> students = api.fetchStudents();

        for (StudentDTO dto : students) {
            StudentProfileCache entity = new StudentProfileCache();

            entity.setStudentId(dto.studentId);
            entity.setFirstName(dto.firstName);
            entity.setLastName(dto.lastName);
            entity.setEmailAddress(dto.emailAddress);
            entity.setBirthDate(dto.birthDate);
            entity.setCollegeCode(dto.collegeCode);
            entity.setCollegeName(dto.collegeName);
            entity.setProgramCode(dto.programCode);
            entity.setProgramName(dto.programName);
            entity.setYearLevel(dto.yearLevel);
            entity.setSectionName(dto.sectionName);
            entity.setScholasticStatus(defaultValue(dto.scholasticStatus, "REGULAR"));
            entity.setSourceUpdatedAt(
                    parseDateTime(dto.updatedAt) != null
                            ? parseDateTime(dto.updatedAt).toLocalDateTime()
                            : null
            );

            studentRepo.save(entity);
        }
    }

    private void syncFaculty() {
        List<FacultyDTO> facultyList = api.fetchFaculty();

        for (FacultyDTO dto : facultyList) {
            FacultyProfileCache entity = new FacultyProfileCache();

            entity.setEmployeeId(dto.employeeId);
            entity.setFirstName(dto.firstName);
            entity.setLastName(dto.lastName);
            entity.setEmailAddress(dto.emailAddress);
            entity.setBirthDate(dto.birthDate);
            entity.setStatus(defaultValue(dto.status, "ACTIVE"));
            entity.setSourceUpdatedAt(
                    parseDateTime(dto.updatedAt) != null
                            ? parseDateTime(dto.updatedAt).toLocalDateTime()
                            : null
            );

            facultyRepo.save(entity);
        }
    }

    private void syncClassOfferings() {
        List<ClassOfferingDTO> offerings = api.fetchClassOfferings();

        for (ClassOfferingDTO dto : offerings) {
            if (dto == null || !notBlank(dto.classOfferingId)) continue;

            ClassOfferingCache entity = new ClassOfferingCache();

            entity.setClassOfferingId(dto.classOfferingId);
            entity.setCourseId(dto.courseId);
            entity.setCourseCode(dto.courseCode);
            entity.setCourseDescription(dto.courseDescription);
            entity.setUnits(dto.units);

            entity.setProgramCode(dto.programCode);
            entity.setYearLevel(dto.yearLevel);
            entity.setSectionName(dto.sectionName);

            entity.setAcademicYear(dto.academicYear);
            entity.setTerm(dto.term);
            entity.setStatus(defaultValue(dto.status, "ACTIVE"));
            entity.setCollegeOffering(dto.collegeOffering);
            entity.setSourceUpdatedAt(
                    parseDateTime(dto.updatedAt) != null
                            ? parseDateTime(dto.updatedAt).toLocalDateTime()
                            : null
            );

            offeringRepo.save(entity);
        }
    }

    private void syncClassEnrollments() {
        List<ClassEnrollmentDTO> enrollments = api.fetchClassEnrollments();

        for (ClassEnrollmentDTO dto : enrollments) {
            if (dto == null || !notBlank(dto.enrollmentId)) continue;

            ClassEnrollmentCache entity = new ClassEnrollmentCache();

            entity.setEnrollmentId(dto.enrollmentId);
            entity.setStudentId(dto.studentId);
            entity.setClassOfferingId(dto.classOfferingId);
            entity.setStatus(defaultValue(dto.status, "ENROLLED"));
            entity.setSourceUpdatedAt(
                    parseDateTime(dto.updatedAt) != null
                            ? parseDateTime(dto.updatedAt).toLocalDateTime()
                            : null
            );

            enrollmentRepo.save(entity);
        }
    }

    private void syncFacultyLoads() {
        List<FacultyLoadDTO> facultyLoads = api.fetchFacultyLoads();

        for (FacultyLoadDTO dto : facultyLoads) {
            if (dto == null || !notBlank(dto.facultyLoadId)) {
                continue;
            }

            FacultyLoadCache entity = new FacultyLoadCache();

            entity.setFacultyLoadId(dto.facultyLoadId);
            entity.setEmployeeId(dto.employeeId);
            entity.setClassOfferingId(dto.classOfferingId);
            entity.setStatus(defaultValue(dto.status, "ACTIVE"));
            entity.setSourceUpdatedAt(
                    parseDateTime(dto.updatedAt) != null
                            ? parseDateTime(dto.updatedAt).toLocalDateTime()
                            : null
            );
            facultyLoadRepo.save(entity);
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }

            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(ZoneId.of("Asia/Manila"))
                    .toOffsetDateTime();

        } catch (Exception e) {
            return null;
        }
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}