package com.example.backend.service.faculty;

import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.faculty.*;
import com.example.backend.dto.faculty.response.FacultyDashboardResponse;
import com.example.backend.entity.enums.ExamDisplayStatus;
import com.example.backend.entity.enums.ExamStatus;
import com.example.backend.entity.exam.*;
import com.example.backend.repository.cache.ClassEnrollmentCacheRepository;
import com.example.backend.repository.exam.*;
import com.example.backend.repository.FacultyRepository;
import com.example.backend.service.exam.ExamStatusService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final ExamWorkspaceRepository examWorkspaceRepository;
    private final ExamStatusService examStatusService;
    private final ClassEnrollmentCacheRepository classEnrollmentCacheRepository;

    public FacultyService(
            FacultyRepository facultyRepository,
            ExamWorkspaceRepository examWorkspaceRepository,
            ExamStatusService examStatusService,
            ClassEnrollmentCacheRepository classEnrollmentCacheRepository
    ) {
        this.facultyRepository = facultyRepository;
        this.examWorkspaceRepository = examWorkspaceRepository;
        this.examStatusService = examStatusService;
        this.classEnrollmentCacheRepository = classEnrollmentCacheRepository;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @TrackActivity(
            module = "FACULTY_DASHBOARD",
            action = "VIEW_DASHBOARD",
            message = "Faculty dashboard viewed"
    )
    public FacultyDashboardResponse getFacultyDashboard(
            String employeeId,
            String role
    ) {

        validateRole(role);

        FacultyDashboardResponse response = new FacultyDashboardResponse();

        response.setProfile(getDashboardProfile(employeeId, role));
        response.setStats(getDashboardStats(employeeId, role));
        response.setActiveExams(getDashboardActiveExams(employeeId, role));
        response.setNeedsReview(getDashboardNeedsReview(employeeId, role));
        response.setRecentSubmissions(getDashboardRecentSubmissions(employeeId, role));

        return response;
    }


    @TrackActivity(
            module = "FACULTY_DASHBOARD",
            action = "VIEW_PROFILE_SUMMARY",
            message = "Faculty dashboard profile viewed"
    )
    public FacultyProfileDTO getDashboardProfile(
            String employeeId,
            String role
    ) {
        validateRole(role);

        return facultyRepository.findFacultyProfile(employeeId);
    }

    @TrackActivity(
            module = "FACULTY_DASHBOARD",
            action = "VIEW_ACTIVE_EXAMS",
            message = "Faculty active exams viewed"
    )
    public List<FacultyExamSummaryDTO> getDashboardActiveExams(
            String employeeId,
            String role
    ) {
        validateRole(role);

        List<FacultyExamSummaryDTO> exams =
                examWorkspaceRepository.findFacultyExamSummaries(employeeId);

        Map<Long, String> sectionMap =
                examWorkspaceRepository.findExamSectionMappings(employeeId)
                        .stream()
                        .collect(Collectors.groupingBy(
                                row -> (Long) row[0],
                                Collectors.mapping(
                                        row -> (String) row[1],
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                list -> String.join(", ", list)
                                        )
                                )
                        ));

        for (FacultyExamSummaryDTO exam : exams) {
            exam.setClassSections(
                    sectionMap.getOrDefault(exam.getExamId(), "—")
            );
        }

        return exams.stream()
                .peek(exam -> {
                    Exam fakeExam = new Exam();

                    fakeExam.setExamId(exam.getExamId());
                    fakeExam.setStatus(ExamStatus.valueOf(exam.getStatus()));
                    fakeExam.setStartDateTime(exam.getStartDateTime());
                    fakeExam.setEndDateTime(exam.getEndDateTime());
                    fakeExam.setExamMode(exam.getExamMode());

                    ExamDisplayStatus computedStatus =
                            examStatusService.getDisplayStatus(
                                    fakeExam,
                                    exam.getTotalAssigned() == null
                                            ? 0
                                            : exam.getTotalAssigned().intValue(),
                                    exam.getSubmittedCount() == null
                                            ? 0
                                            : exam.getSubmittedCount().intValue()
                            );

                    exam.setStatus(computedStatus.name());
                })
                .filter(exam -> isDashboardActiveStatus(exam.getStatus()))
                .toList();
    }

    @TrackActivity(
            module = "FACULTY_DASHBOARD",
            action = "VIEW_RECENT_SUBMISSIONS",
            message = "Faculty recent submissions viewed"
    )
    public List<FacultySubmissionSummaryDTO> getDashboardRecentSubmissions(
            String employeeId,
            String role
    ) {

        validateRole(role);

        return facultyRepository.findRecentSubmissions(employeeId)
                .stream()
                .limit(5)
                .toList();
    }

    @TrackActivity(
            module = "FACULTY_DASHBOARD",
            action = "VIEW_NEEDS_REVIEW",
            message = "Faculty needs-review list viewed"
    )
    public List<FacultyViolationReviewDTO> getDashboardNeedsReview(
            String employeeId,
            String role
    ) {

        validateRole(role);

        return facultyRepository.findViolationsForReview(employeeId)
                .stream()
                .limit(5)
                .toList();
    }

    @TrackActivity(
            module = "FACULTY_DASHBOARD",
            action = "VIEW_STATS",
            message = "Faculty dashboard stats viewed"
    )
    public FacultyDashboardStatsDTO getDashboardStats(
            String employeeId,
            String role
    ) {
        validateRole(role);

        List<FacultyExamSummaryDTO> activeExams =
                getDashboardActiveExams(employeeId, role);

        List<FacultyClassDTO> classes =
                getFacultyClasses(employeeId, role);

        long totalStudents =
                classEnrollmentCacheRepository.countDistinctStudentsForFacultyDashboard(employeeId);

        long reviewQueueCount = getDashboardNeedsReview(employeeId, role)
                .stream()
                .mapToLong(r -> r.getStudentCount() == null ? 0 : r.getStudentCount())
                .sum();

        return new FacultyDashboardStatsDTO(
                (long) activeExams.size(),
                (long) classes.size(),
                totalStudents,
                reviewQueueCount
        );
    }

    // =========================================================
    // EXAMS
    // =========================================================

    @TrackActivity(
            module = "FACULTY_EXAMS",
            action = "VIEW_EXAMS",
            message = "Faculty exams viewed"
    )
    public List<FacultyExamSummaryDTO> getFacultyExams(
            String employeeId,
            String role
    ) {

        validateRole(role);

        return examWorkspaceRepository.findFacultyExamSummaries(employeeId);
    }

    // =========================================================
    // RESULTS
    // =========================================================

    @TrackActivity(
            module = "FACULTY_CLASSES",
            action = "VIEW_CLASSES",
            message = "Faculty assigned classes viewed"
    )
    public List<FacultyClassDTO> getFacultyClasses(
            String employeeId,
            String role
    ) {
        validateRole(role);

        return facultyRepository.findAssignedClasses(employeeId);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean isDashboardActiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }

        return switch (status.toUpperCase()) {
            case "DRAFT",
                 "ASSIGNED",
                 "PUBLISHED",
                 "SCHEDULED",
                 "ONGOING" -> true;

            case "COMPLETED",
                 "EXPIRED",
                 "CANCELLED" -> false;

            default -> false;
        };
    }

    private void validateRole(String role) {

        if (role == null) {
            throw new RuntimeException("Unauthorized access.");
        }

        boolean allowed =
                "FACULTY".equalsIgnoreCase(role) ||
                        "ADMIN".equalsIgnoreCase(role);

        if (!allowed) {
            throw new RuntimeException(
                    "Only faculty or admin users can access this resource."
            );
        }
    }
}