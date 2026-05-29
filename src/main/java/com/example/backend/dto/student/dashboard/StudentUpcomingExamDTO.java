package com.example.backend.dto.student.dashboard;

import com.example.backend.entity.enums.ExamAttemptStatus;
import com.example.backend.entity.enums.ExamMode;
import com.example.backend.entity.enums.ExamStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
public class StudentUpcomingExamDTO implements Serializable {
    private Long examId;
    private String title;
    private String faculty;
    private String courseCode;
    private String courseDescription;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private Integer timeLimitMinutes;
    private ExamMode examMode;
    private ExamStatus status;
    private Long questionCount;
    private ExamAttemptStatus attemptStatus;

    public StudentUpcomingExamDTO(
            Long examId,
            String title,
            String faculty,
            String courseCode,
            String courseDescription,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime,
            Integer timeLimitMinutes,
            ExamMode examMode,
            ExamStatus status,
            Long questionCount,
            ExamAttemptStatus attemptStatus
    ) {
        this.examId = examId;
        this.title = title;
        this.faculty = faculty;
        this.courseCode = courseCode;
        this.courseDescription = courseDescription;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.timeLimitMinutes = timeLimitMinutes;
        this.examMode = examMode;
        this.status = status;
        this.questionCount = questionCount;
        this.attemptStatus = attemptStatus;
    }

}
