package com.example.backend.service.faculty;

import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.exam.request.EssayRubricRequest;
import com.example.backend.dto.exam.response.EssayRubricScoreResponse;
import com.example.backend.dto.faculty.*;
import com.example.backend.dto.faculty.response.FacultyAttemptReviewResponse;
import com.example.backend.dto.faculty.response.FacultyDashboardResponse;
import com.example.backend.dto.faculty.response.FacultyExamDetailResponse;
import com.example.backend.dto.faculty.response.SimpleMessageResponse;
import com.example.backend.entity.exam.*;
import com.example.backend.repository.exam.*;
import com.example.backend.repository.FacultyRepository;
import com.example.backend.service.core.SystemActivityLogService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final EssayRubricRepository essayRubricRepository;
    private final EssayRubricScoreRepository rubricScoreRepository;
    private final SystemActivityLogService activityLogService;
    private final ExamWorkspaceRepository examWorkspaceRepository;

    public FacultyService(
            FacultyRepository facultyRepository,
            ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository,
            ExamAnswerRepository examAnswerRepository,
            ExamAttemptRepository examAttemptRepository,
            EssayRubricRepository essayRubricRepository,
            EssayRubricScoreRepository rubricScoreRepository,
            SystemActivityLogService activityLogService, ExamWorkspaceRepository examWorkspaceRepository
    ) {
        this.facultyRepository = facultyRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examAnswerRepository = examAnswerRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.essayRubricRepository = essayRubricRepository;
        this.rubricScoreRepository = rubricScoreRepository;
        this.activityLogService = activityLogService;
        this.examWorkspaceRepository = examWorkspaceRepository;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @TrackActivity(
            module = "FACULTY",
            action = "GET_DASHBOARD"
    )
    public FacultyDashboardResponse getFacultyDashboard(
            String employeeId,
            String role
    ) {

        validateRole(role);

        FacultyDashboardResponse response =
                new FacultyDashboardResponse();

        response.setProfile(
                getDashboardProfile(employeeId, role)
        );

        response.setStats(
                getDashboardStats(employeeId, role)
        );

        response.setActiveExams(
                getDashboardActiveExams(employeeId, role)
        );

        response.setNeedsReview(
                getDashboardNeedsReview(employeeId, role)
        );

        response.setRecentSubmissions(
                getDashboardRecentSubmissions(employeeId, role)
        );

        return response;
    }


    @TrackActivity(
            module = "FACULTY",
            action = "GET_DASHBOARD_PROFILE"
    )
    public FacultyProfileDTO getDashboardProfile(
            String employeeId,
            String role
    ) {
        validateRole(role);

        return facultyRepository.findFacultyProfile(employeeId);
    }

    @TrackActivity(
            module = "FACULTY",
            action = "GET_DASHBOARD_ACTIVE_EXAMS"
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
                .filter(exam ->
                        !"COMPLETED".equalsIgnoreCase(exam.getStatus()) &&
                                !"CANCELLED".equalsIgnoreCase(exam.getStatus())
                )
                .toList();
    }

    @TrackActivity(
            module = "FACULTY",
            action = "GET_DASHBOARD_RECENT_SUBMISSIONS"
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
            module = "FACULTY",
            action = "GET_DASHBOARD_NEEDS_REVIEW"
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
            module = "FACULTY",
            action = "GET_DASHBOARD_STATS"
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

        long totalStudents = classes.stream()
                .mapToLong(c -> c.getEnrolledCount() == null ? 0 : c.getEnrolledCount())
                .sum();

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
            module = "FACULTY",
            action = "GET_FACULTY_EXAMS"
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
            module = "FACULTY",
            action = "GET_FACULTY_CLASSES"
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