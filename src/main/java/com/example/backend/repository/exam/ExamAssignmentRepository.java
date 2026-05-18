package com.example.backend.repository.exam;

import com.example.backend.entity.exam.ExamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, Long> {
    List<ExamAssignment> findByExamExamId(Long examId);
    List<ExamAssignment> findByExamExamIdIn(List<Long> examIds);
    void deleteByExamExamId(Long examId);
    @Query("""
    SELECT ea.classOfferingId
    FROM ExamAssignment ea
    WHERE ea.exam.examId = :examId
""")
    List<String> findClassOfferingIdsByExamId(@Param("examId") Long examId);
}