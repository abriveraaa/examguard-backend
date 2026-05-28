package com.example.backend.service.core;

import com.example.backend.audit.TrackActivity;
import com.example.backend.entity.cache.FacultyProfileCache;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.cache.FacultyProfileCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.UserAccessRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class RegistrarAccessSyncService {

    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final UserAccessRepository userAccessRepository;

    @TrackActivity(
            module = "REGISTRAR_SYNC",
            action = "SYNC_ACCESS_STATUS",
            message = "Registrar access synchronization started"
    )
    public String refreshAndSyncAccessStatus() {
        List<StudentProfileCache> students = studentProfileCacheRepository.findAll();
        List<FacultyProfileCache> facultyList = facultyProfileCacheRepository.findAll();

        int studentDeactivated = syncStudents(students);
        int facultyDeactivated = syncFaculty(facultyList);

        return "Sync completed. Deactivated " + studentDeactivated +
                " student account(s) and " + facultyDeactivated + " faculty account(s).";
    }

    private int syncStudents(List<StudentProfileCache> students) {
        int count = 0;
        if (students == null) return 0;

        for (StudentProfileCache student : students) {
            if (student == null || student.getStudentId() == null) continue;

            Optional<UserAccess> userOpt = findUserAccessBySchoolId(student.getStudentId());
            if (userOpt.isEmpty()) continue;

            UserAccess user = userOpt.get();

            if (!"STUDENT".equalsIgnoreCase(user.getRole())) continue;

            String scholasticStatus = student.getScholasticStatus();

            boolean registrarInactive = "INACTIVE".equalsIgnoreCase(scholasticStatus);

            if (registrarInactive && user.isActive()) {
                user.setActive(false);
                user.setBlocked(false);
                user.setDeactivationReason("REGISTRAR_INACTIVE");
                user.setEligibleForReactivation(false);

                userAccessRepository.save(user);
                count++;

            } else if (!registrarInactive
                    && !user.isActive()
                    && "REGISTRAR_INACTIVE".equalsIgnoreCase(user.getDeactivationReason())) {

                user.setEligibleForReactivation(true);
                userAccessRepository.save(user);
            }
        }

        return count;
    }

    private int syncFaculty(List<FacultyProfileCache> facultyList) {
        int count = 0;
        if (facultyList == null) return 0;

        for (FacultyProfileCache faculty : facultyList) {
            if (faculty == null || faculty.getEmployeeId() == null) continue;

            Optional<UserAccess> userOpt = findUserAccessBySchoolId(faculty.getEmployeeId());
            if (userOpt.isEmpty()) continue;

            UserAccess user = userOpt.get();

            if (!"FACULTY".equalsIgnoreCase(user.getRole())) continue;

            boolean registrarActive = "ACTIVE".equalsIgnoreCase(faculty.getStatus());

            if (!registrarActive && user.isActive()) {
                user.setActive(false);
                user.setBlocked(false);
                user.setDeactivationReason("REGISTRAR_INACTIVE");
                user.setEligibleForReactivation(false);
                userAccessRepository.save(user);
                count++;
            } else if (registrarActive
                    && !user.isActive()
                    && "REGISTRAR_INACTIVE".equalsIgnoreCase(user.getDeactivationReason())) {

                user.setEligibleForReactivation(true);
                userAccessRepository.save(user);
            }
        }

        return count;
    }

    private Optional<UserAccess> findUserAccessBySchoolId(String schoolId) {
        String safeId = safe(schoolId);

        Optional<UserAccess> exact = userAccessRepository.findBySchoolId(safeId);
        if (exact.isPresent()) return exact;

        String normalizedInput = normalizeId(safeId);

        return userAccessRepository.findAll()
                .stream()
                .filter(user -> normalizeId(user.getSchoolId()).equals(normalizedInput))
                .findFirst();
    }

    @TrackActivity(
            module = "REGISTRAR_SYNC",
            action = "VIEW_REACTIVATION_ELIGIBLE_USERS",
            message = "Viewed users eligible for reactivation"
    )
    public List<UserAccess> getEligibleForReactivationUsers() {
        return userAccessRepository.findByEligibleForReactivationTrue();
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}