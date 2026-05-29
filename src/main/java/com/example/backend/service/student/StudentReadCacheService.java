package com.example.backend.service.student;

import com.example.backend.dto.core.CurrentTermDTO;
import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.dto.student.dashboard.*;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.repository.cache.ClassOfferingCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.StudentExamRepository;
import com.example.backend.repository.exam.ExamRepository;
import com.example.backend.repository.exam.ExamViolationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentReadCacheService {

    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final ClassOfferingCacheRepository classOfferingCacheRepository;
    private final ExamRepository examRepository;
    private final StudentExamRepository studentExamRepository;
    private final ExamViolationLogRepository violationLogRepository;

    @Cacheable(value = "studentDashboardProfile", key = "#studentId")
    public StudentProfileDTO getProfile(String studentId) {
        StudentProfileCache profile = studentProfileCacheRepository
                .findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student profile not found."));

        List<CurrentTermDTO> currentTerm = classOfferingCacheRepository.findCurrentTerm();

        String currentTermText = "";
        if (!currentTerm.isEmpty()) {
            CurrentTermDTO t = currentTerm.get(0);
            currentTermText = t.getTerm() + ", AY " + t.getAcademicYear();
        }

        return StudentProfileDTO.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .schoolId(profile.getStudentId())
                .emailAddress(profile.getEmailAddress())
                .programCode(profile.getProgramCode())
                .programName(profile.getProgramName())
                .yearLevel(profile.getYearLevel())
                .sectionName(profile.getSectionName())
                .collegeCode(profile.getCollegeCode())
                .collegeName(profile.getCollegeName())
                .currentTerm(currentTermText)
                .integrityStatus(null)
                .integritySubtitle(null)
                .profileImageUrl(null)
                .build();
    }

    @Cacheable(value = "studentDashboardUpcomingExams", key = "#studentId")
    public List<StudentUpcomingExamDTO> getUpcomingExams(String studentId) {
        return examRepository.findPublishedAssignedExamsForStudent(studentId);
    }

    @Cacheable(value = "studentDashboardResults", key = "#studentId")
    public List<StudentResultSummaryDTO> getResults(String studentId) {
        return examRepository.findReleasedUnviewedResultsForStudent(
                studentId,
                PageRequest.of(0, 4)
        );
    }

    @Cacheable(value = "studentDashboardViolations", key = "#studentId")
    public List<StudentViolationSummaryDTO> getViolations(String studentId) {
        return violationLogRepository.findReviewedUnviewedViolationsForStudent(
                studentId,
                PageRequest.of(0, 4)
        );
    }

    @Cacheable(value = "studentExamsRaw", key = "#studentId")
    public List<StudentExamCardDTO> getStudentExams(String studentId) {
        return studentExamRepository.findStudentExamCards(studentId);

    }
}