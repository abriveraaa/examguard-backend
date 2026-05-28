package com.example.backend.service.student;

import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.entity.enums.ExamMode;
import com.example.backend.repository.core.StudentExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentExamService {

    private final StudentExamRepository studentExamRepository;

    @TrackActivity(
            module = "STUDENT_EXAMS",
            action = "VIEW_EXAM_CARDS",
            message = "Student viewed exam cards"
    )
    public List<StudentExamCardDTO> getStudentExamCards(String studentId) {

        OffsetDateTime now = OffsetDateTime.now();

        List<StudentExamCardDTO> exams =
                studentExamRepository.findStudentExamCards(studentId);

        for (StudentExamCardDTO exam : exams) {
            String status = computeStatus(exam, now);

            exam.setStatus(status);
            exam.setActionable(isActionable(status));
        }

        return exams;
    }

    private String computeStatus(StudentExamCardDTO exam, OffsetDateTime now) {

        String attemptStatus = exam.getAttemptStatus();

        if (Boolean.TRUE.equals(exam.getResultsReleased())) {
            return "RESULTS RELEASED";
        }

        if ("SUBMITTED".equalsIgnoreCase(attemptStatus)
                || "AUTO_SUBMITTED".equalsIgnoreCase(attemptStatus)) {
            return "PENDING REVIEW";
        }

        OffsetDateTime start = exam.getStartDateTime();
        OffsetDateTime end = exam.getEndDateTime();

        if (start == null || end == null) {
            return "UPCOMING";
        }

        int durationMinutes =
                exam.getDurationMinutes() == null
                        ? 60
                        : exam.getDurationMinutes();

        OffsetDateTime effectiveEnd =
                exam.getMode() == ExamMode.SYNCHRONOUS
                        ? start.plusMinutes(durationMinutes)
                        : end;

        if (now.isBefore(start)) {
            return "UPCOMING";
        }

        if ("IN_PROGRESS".equalsIgnoreCase(attemptStatus)) {
            return now.isBefore(effectiveEnd)
                    ? "ONGOING"
                    : "DID NOT TAKE";
        }

        if (now.isBefore(effectiveEnd)) {
            return "ONGOING";
        }

        return "DID NOT TAKE";
    }

    private Boolean isActionable(String status) {
        return switch (status) {
            case "ONGOING", "RESULTS RELEASED" -> true;
            default -> false;
        };
    }
}