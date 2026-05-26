package com.example.backend.service.faculty;

import com.example.backend.dto.faculty.reports.*;
import com.example.backend.repository.report.FacultyReportsRepository;
import com.example.backend.report.faculty.ClassRecordExportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyReportsService {

    private final FacultyReportsRepository facultyReportsRepository;
    private final ClassRecordExportService classRecordExportService;

    // ==================
    // EXAM RESULTS SUMMARY
    // ==================

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

    public List<ExamSubmissionBreakdownDTO> getSubmissionBreakdown(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return facultyReportsRepository.getSubmissionBreakdownRaw(
                        facultyId,
                        academicYear,
                        term,
                        normalize(courseCode),
                        normalize(classOfferingId)
                )
                .stream()
                .map(row -> new ExamSubmissionBreakdownDTO(
                        ((Number) row[0]).longValue(),
                        String.valueOf(row[1]),
                        String.valueOf(row[2]),
                        ((Number) row[3]).longValue()
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

    // ==================
    // CLASS RECORD
    // ==================

    public byte[] exportClassRecordPdf(
            String facultyId,
            String classOfferingId
    ) {
        String normalizedClassOfferingId = normalize(classOfferingId);
        List<Object[]> sectionInfoRows =
                facultyReportsRepository.findClassRecordSectionInfo(
                        facultyId,
                        normalizedClassOfferingId
                );

        Object[] sectionInfo = sectionInfoRows == null || sectionInfoRows.isEmpty() ? null : sectionInfoRows.get(0);

        String courseCode = "";
        String sectionLabel = normalizedClassOfferingId;
        String collegeOffering = "";

        if (sectionInfo != null && sectionInfo.length >= 3) {
            courseCode = sectionInfo[0] == null ? "-" : (sectionInfo[0] + ": "+ sectionInfo[1]).toUpperCase();
            sectionLabel = sectionInfo[2] == null ? "" : String.valueOf(sectionInfo[2] );
            collegeOffering = sectionInfo[3] == null ? " " : String.valueOf(sectionInfo[3]);
        }

        List<Object[]> examRows = facultyReportsRepository.findClassRecordExamColumns(facultyId, normalizedClassOfferingId);

        AtomicInteger examCounter = new AtomicInteger(1);

        List<ClassRecordColumnDTO> columns =
                examRows.stream()
                        .map(row ->
                                new ClassRecordColumnDTO(
                                        ((Number) row[0]).longValue(),
                                        "EXAM " + examCounter.getAndIncrement(),
                                        String.valueOf(row[1]),
                                        toBigDecimal(row[2]),
                                        convertToOffsetDateTime(row[3]),
                                        convertToOffsetDateTime(row[4])
                                )
                        )
                        .toList();

        List<Long> examIds =
                columns.stream()
                        .map(ClassRecordColumnDTO::examId)
                        .toList();

        List<Object[]> studentRows =
                facultyReportsRepository.findClassRecordStudents(
                        facultyId,
                        normalizedClassOfferingId
                );

        Map<String, ClassRecordStudentRowDTO> studentMap =
                new LinkedHashMap<>();

        for (Object[] row : studentRows) {
            String studentId =
                    String.valueOf(row[0]);

            String studentName =
                    row[1] == null || String.valueOf(row[1]).isBlank()
                            ? studentId
                            : String.valueOf(row[1]);

            String studentSection =
                    row[2] == null || String.valueOf(row[2]).isBlank()
                            ? sectionLabel
                            : String.valueOf(row[2]);

            studentMap.put(
                    studentId,
                    new ClassRecordStudentRowDTO(
                            studentId,
                            studentName,
                            studentSection,
                            new LinkedHashMap<>(),
                            BigDecimal.ZERO
                    )
            );
        }

        if (!examIds.isEmpty()) {
            List<Object[]> scoreRows =
                    facultyReportsRepository.findClassRecordScores(
                            facultyId,
                            normalizedClassOfferingId,
                            examIds
                    );

            for (Object[] row : scoreRows) {
                String studentId =
                        String.valueOf(row[0]);

                Long examId =
                        ((Number) row[1]).longValue();

                ClassRecordStudentRowDTO student =
                        studentMap.get(studentId);

                if (student == null) {
                    continue;
                }

                student.scoresByExamId()
                        .put(
                                examId,
                                new ClassRecordScoreCellDTO(
                                        toNullableBigDecimal(row[2]),
                                        toBigDecimal(row[3]),
                                        toNullableBigDecimal(row[4]),
                                        row[5] == null
                                                ? "DID_NOT_TAKE"
                                                : String.valueOf(row[5])
                                )
                        );
            }
        }

        List<ClassRecordStudentRowDTO> finalRows =
                studentMap.values()
                        .stream()
                        .map(student ->
                                new ClassRecordStudentRowDTO(
                                        student.studentId(),
                                        student.studentName(),
                                        student.sectionName(),
                                        student.scoresByExamId(),
                                        computeAverage(
                                                student,
                                                columns
                                        )
                                )
                        )
                        .toList();

        return classRecordExportService.generatePdf(
                courseCode,
                List.of(sectionLabel),
                columns,
                finalRows,
                collegeOffering,
                facultyId
        );
    }

    // ==================
    // METHOD HELPER
    // ==================

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.startsWith("All")) return null;
        return value;
    }

    private BigDecimal computeAverage(
            ClassRecordStudentRowDTO student,
            List<ClassRecordColumnDTO> columns
    ) {
        if (columns == null || columns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total =
                BigDecimal.ZERO;

        int count = 0;

        for (ClassRecordColumnDTO column : columns) {
            ClassRecordScoreCellDTO score =
                    student.scoresByExamId()
                            .get(column.examId());

            if (score == null) {
                total = total.add(BigDecimal.ZERO);
                count++;
                continue;
            }

            if ("DID_NOT_TAKE".equalsIgnoreCase(score.status())) {
                total = total.add(BigDecimal.ZERO);
                count++;
                continue;
            }

            if ("PENDING".equalsIgnoreCase(score.status())) {
                continue;
            }

            if (score.percentage() == null) {
                continue;
            }

            total = total.add(score.percentage());
            count++;
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return total.divide(
                BigDecimal.valueOf(count),
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal toBigDecimal(
            Object value
    ) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(
                    n.doubleValue()
            );
        }

        return new BigDecimal(
                String.valueOf(value)
        );
    }

    private BigDecimal toNullableBigDecimal(
            Object value
    ) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        return new BigDecimal(String.valueOf(value));
    }

    private OffsetDateTime convertToOffsetDateTime(
            Object value
    ) {
        if (value == null) {
            return null;
        }

        if (value instanceof OffsetDateTime odt) {
            return odt;
        }

        if (value instanceof Instant instant) {
            return instant.atOffset(
                    ZoneOffset.of("+08:00")
            );
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant()
                    .atOffset(
                            ZoneOffset.of("+08:00")
                    );
        }

        return null;
    }
}