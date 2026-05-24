package com.example.backend.service.faculty;

import com.example.backend.dto.faculty.reports.*;
import com.example.backend.repository.report.FacultyReportsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyReportsService {

    private final FacultyReportsRepository facultyReportsRepository;

    public FacultyReportSummaryDTO getSummary(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId,
            Long examId
    ) {
        String normalizedCourseCode = normalize(courseCode);
        String normalizedClassOfferingId = normalize(classOfferingId);

        Double averageScore = facultyReportsRepository.getAverageScore(
                facultyId,
                academicYear,
                term,
                normalizedCourseCode,
                normalizedClassOfferingId,
                examId
        );

        Double submissionRate = facultyReportsRepository.getSubmissionRate(
                facultyId,
                academicYear,
                term,
                normalizedCourseCode,
                normalizedClassOfferingId,
                examId
        );

        Long totalViolations = facultyReportsRepository.getTotalViolations(
                facultyId,
                academicYear,
                term,
                normalizedCourseCode,
                normalizedClassOfferingId,
                examId
        );

        Long pendingReview = facultyReportsRepository.countViolationsByReviewStatus(
                facultyId,
                academicYear,
                term,
                normalizedCourseCode,
                normalizedClassOfferingId,
                examId,
                "PENDING_REVIEW"
        );

        Long penalized = facultyReportsRepository.countViolationsByReviewStatus(
                facultyId,
                academicYear,
                term,
                normalizedCourseCode,
                normalizedClassOfferingId,
                examId,
                "PENALIZED"
        );

        return new FacultyReportSummaryDTO(
                averageScore == null ? 0.0 : averageScore,
                submissionRate == null ? 0.0 : submissionRate,
                totalViolations == null ? 0L : totalViolations,
                pendingReview == null ? 0L : pendingReview,
                penalized == null ? 0L : penalized
        );
    }

    public List<ExamParticipationDTO> getParticipation(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return facultyReportsRepository.getParticipation(
                facultyId,
                academicYear,
                term,
                normalize(courseCode),
                normalize(classOfferingId)
        );
    }

    public List<SubmissionStatusDTO> getSubmissionStatus(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId,
            Long examId
    ) {
        return facultyReportsRepository.getSubmissionStatusRaw(
                        facultyId,
                        academicYear,
                        term,
                        courseCode,
                        classOfferingId,
                        examId
                )
                .stream()
                .map(row -> new SubmissionStatusDTO(
                        row.getStatus(),
                        row.getCount()
                ))
                .toList();
    }

    public List<ViolationTypeDTO> getViolations(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId,
            Long examId
    ) {
        return facultyReportsRepository.getViolations(
                facultyId,
                academicYear,
                term,
                normalize(courseCode),
                normalize(classOfferingId),
                examId
        );
    }

    public List<ReportExamOptionDTO> getExamOptions(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return facultyReportsRepository.getExamOptions(
                facultyId,
                academicYear,
                term,
                normalize(courseCode),
                normalize(classOfferingId)
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.startsWith("All")) return null;
        return value;
    }
}