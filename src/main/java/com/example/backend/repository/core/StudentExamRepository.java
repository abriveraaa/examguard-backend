package com.example.backend.repository.core;

import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.entity.exam.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface StudentExamRepository extends JpaRepository<Exam, Long> {

    @Query("""
    SELECT DISTINCT new com.example.backend.dto.student.StudentExamCardDTO(
        e.examId,
        e.title,
        co.courseCode,
        co.courseDescription,
        co.term,
        co.academicYear,
        co.status AS classOfferingStatus,
        CAST(e.examMode AS string),

        CASE
            WHEN fp.employeeId IS NOT NULL
            THEN CONCAT(fp.firstName, ' ', fp.lastName)
            ELSE 'Faculty'
        END,

        e.timeLimitMinutes,
        e.startDateTime,
        e.endDateTime,

        CAST(a.status AS string),
        a.reviewStatus,

        e.resultsReleased
    )

    FROM ExamAssignment ea

    JOIN ea.exam e

    JOIN ClassEnrollmentCache ce
        ON ce.classOfferingId = ea.classOfferingId

    JOIN ClassOfferingCache co
        ON co.classOfferingId = ea.classOfferingId

    LEFT JOIN FacultyLoadCache fl
        ON fl.classOfferingId = ea.classOfferingId

    LEFT JOIN FacultyProfileCache fp
        ON fp.employeeId = fl.employeeId

    LEFT JOIN ExamAttempt a
        ON a.examId = e.examId
       AND a.studentId = ce.studentId

    WHERE ce.studentId = :studentId
      AND e.published = true
      AND e.status <>
          com.example.backend.entity.enums.ExamStatus.CANCELLED

    ORDER BY e.startDateTime ASC
""")
    List<StudentExamCardDTO> findStudentExamCards(
            @Param("studentId") String studentId
    );
}
