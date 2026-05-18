package com.example.backend.entity.exam;

import com.example.backend.entity.enums.ExamStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exam_assignment")
public class ExamAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_assignment_assignment_id_seq")
    @SequenceGenerator(
            name = "exam_assignment_assignment_id_seq",
            sequenceName = "exam_assignment_assignment_id_seq",
            allocationSize = 50
    )
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "class_offering_id")
    private String classOfferingId;

    @Column(name = "assigned_by")
    private String assignedBy;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getAssignmentId() { return assignmentId; }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public Long getExamId() {
        return exam != null ? exam.getExamId() : null;
    }

    public String getClassOfferingId() { return classOfferingId; }
    public void setClassOfferingId(String classOfferingId) { this.classOfferingId = classOfferingId; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public ExamStatus getStatus() { return status; }
    public void setStatus(ExamStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}