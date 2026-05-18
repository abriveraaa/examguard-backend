package com.example.backend.service.student;

import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.repository.core.StudentExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentExamService {

    private final StudentExamRepository studentExamRepository;

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

        if ("SUBMITTED".equalsIgnoreCase(attemptStatus) || "AUTO_SUBMITTED".equalsIgnoreCase(attemptStatus)) {
            return "PENDING REVIEW";
        }

        if (!now.isBefore(exam.getStartDateTime()) && !now.isAfter(exam.getEndDateTime())) {
            return "ONGOING";
        }

        if (now.isBefore(exam.getStartDateTime())) {
            return "UPCOMING";
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