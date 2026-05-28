package com.example.backend.service.exam;

import com.example.backend.audit.ActivityTarget;
import com.example.backend.audit.ActivityTargetType;
import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.exam.request.AssignExamRequest;
import com.example.backend.dto.exam.result.AssignExamResult;
import com.example.backend.entity.enums.ExamStatus;
import com.example.backend.entity.exam.Exam;
import com.example.backend.entity.exam.ExamAssignment;
import com.example.backend.repository.cache.ClassOfferingCacheRepository;
import com.example.backend.repository.cache.FacultyLoadCacheRepository;
import com.example.backend.repository.exam.ExamAssignmentRepository;
import com.example.backend.repository.exam.ExamRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ExamAssignmentService {

    private final ExamRepository examRepository;
    private final ExamAssignmentRepository assignmentRepository;
    private final ClassOfferingCacheRepository classOfferingRepository;
    private final FacultyLoadCacheRepository facultyLoadRepository;

    @TrackActivity(
            module = "EXAM_ASSIGNMENT",
            action = "ASSIGN_EXAM",
            message = "Exam assignment attempted"
    )
    @Transactional
    public AssignExamResult assignExam(

            @ActivityTarget(ActivityTargetType.EXAM_ID)
            Long examId,

            AssignExamRequest request
    ) {

        Exam exam = examRepository.findById(examId).orElse(null);

        if (exam == null) {
            return new AssignExamResult(false, "Exam not found.", examId, 0);
        }

        if (request == null) {
            return new AssignExamResult(false, "Invalid assignment request.", examId, 0);
        }

        if (isBlank(request.getAssignedBy()) || isBlank(request.getAssignedByRole())) {
            return new AssignExamResult(false, "Assigned by and role are required.", examId, 0);
        }

        if (request.getClassOfferingIds() == null || request.getClassOfferingIds().isEmpty()) {
            return new AssignExamResult(false, "Please select at least one class offering.", examId, 0);
        }

        if (request.getStartTime() != null
                && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            return new AssignExamResult(false, "End time must be after start time.", examId, 0);
        }

        String role = request.getAssignedByRole().trim().toUpperCase();
        String assignedBy = request.getAssignedBy().trim();

        int assignedCount = 0;

        for (String classOfferingId : request.getClassOfferingIds()) {
            if (isBlank(classOfferingId)) continue;

            String safeClassOfferingId = classOfferingId.trim();

            if (!classOfferingRepository.existsById(safeClassOfferingId)) {
                return new AssignExamResult(false, "Class offering not found: " + safeClassOfferingId, examId, assignedCount);
            }

            if ("FACULTY".equals(role)) {
                boolean allowed = facultyLoadRepository
                        .existsByEmployeeIdAndClassOfferingIdAndStatusIgnoreCase(
                                assignedBy,
                                safeClassOfferingId,
                                "ACTIVE"
                        );

                if (!allowed) {
                    return new AssignExamResult(
                            false,
                            "Faculty is not assigned to class offering: " + safeClassOfferingId,
                            examId,
                            assignedCount
                    );
                }
            }

            ExamAssignment assignment = new ExamAssignment();
            assignment.setExam(exam);
            assignment.setClassOfferingId(safeClassOfferingId);
            assignment.setAssignedBy(assignedBy);
            assignment.setStartTime(request.getStartTime());
            assignment.setEndTime(request.getEndTime());
            assignment.setStatus(ExamStatus.PUBLISHED);

            assignmentRepository.save(assignment);
            assignedCount++;
        }

        return new AssignExamResult(true, "Exam assigned successfully.", examId, assignedCount);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}