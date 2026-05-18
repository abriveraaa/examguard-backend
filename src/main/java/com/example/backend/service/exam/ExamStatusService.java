package com.example.backend.service.exam;

import com.example.backend.entity.exam.Exam;
import com.example.backend.entity.enums.ExamDisplayStatus;
import com.example.backend.entity.enums.ExamStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class ExamStatusService {

    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");

    public ExamDisplayStatus getDisplayStatus(Exam exam) {

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

        if (exam.getStartDateTime() != null && now.isBefore(exam.getStartDateTime())) {
            return ExamDisplayStatus.SCHEDULED;
        }

        if (exam.getEndDateTime() != null && now.isAfter(exam.getEndDateTime())) {
            return ExamDisplayStatus.EXPIRED;
        }

        return ExamDisplayStatus.ONGOING;
    }
}