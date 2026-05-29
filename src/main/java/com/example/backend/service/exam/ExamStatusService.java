package com.example.backend.service.exam;

import com.example.backend.entity.exam.Exam;
import com.example.backend.entity.enums.ExamDisplayStatus;
import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.enums.ExamStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class ExamStatusService {

    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");

    public ExamDisplayStatus getDisplayStatus(
            Exam exam,
            Integer totalAssignedStudents,
            Integer submittedStudents
    ) {
        if (exam == null) {
            return ExamDisplayStatus.DRAFT;
        }

        if (exam.getStatus() == ExamStatus.DRAFT) {
            return ExamDisplayStatus.DRAFT;
        }

        if (exam.getStatus() == ExamStatus.CANCELLED) {
            return ExamDisplayStatus.CANCELLED;
        }

        if (exam.getStatus() == ExamStatus.COMPLETED) {
            return ExamDisplayStatus.COMPLETED;
        }

        OffsetDateTime now = OffsetDateTime.now(MANILA_ZONE);
        OffsetDateTime start = toManila(exam.getStartDateTime());

        ExamMode mode = exam.getExamMode() == null
                ? ExamMode.SYNCHRONOUS
                : exam.getExamMode();

        OffsetDateTime end = computeActualEnd(exam, start, mode);

        if (start == null || end == null) {
            return ExamDisplayStatus.SCHEDULED;
        }

        boolean hasAssignedStudents =
                totalAssignedStudents != null
                        && totalAssignedStudents > 0;

        boolean allStudentsFinished =
                hasAssignedStudents
                        && submittedStudents != null
                        && submittedStudents >= totalAssignedStudents;

        if (allStudentsFinished) {
            return ExamDisplayStatus.COMPLETED;
        }

        if (now.isBefore(start)) {
            return ExamDisplayStatus.SCHEDULED;
        }

        if (!now.isAfter(end)) {
            return ExamDisplayStatus.ONGOING;
        }

        return ExamDisplayStatus.EXPIRED;
    }

    private OffsetDateTime computeActualEnd(
            Exam exam,
            OffsetDateTime start,
            ExamMode mode
    ) {
        if (start == null) {
            return null;
        }

        if (mode == ExamMode.SYNCHRONOUS) {
            int minutes = exam.getTimeLimitMinutes() == null
                    ? 60
                    : exam.getTimeLimitMinutes();

            return start.plusMinutes(minutes);
        }

        return toManila(exam.getEndDateTime());
    }

    private OffsetDateTime toManila(OffsetDateTime value) {
        if (value == null) {
            return null;
        }

        return value
                .atZoneSameInstant(MANILA_ZONE)
                .toOffsetDateTime();
    }
}