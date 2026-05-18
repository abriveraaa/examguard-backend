package com.example.backend.entity.cache;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_enrollment_cache")
public class ClassEnrollmentCache {

    @Id
    @Column(name = "enrollment_id")
    private String enrollmentId;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "class_offering_id")
    private String classOfferingId;

    @Column(name = "status")
    private String status;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @PrePersist
    @PreUpdate
    public void beforeSave() {
        this.lastSyncedAt = LocalDateTime.now();
    }

    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassOfferingId() { return classOfferingId; }
    public void setClassOfferingId(String classOfferingId) { this.classOfferingId = classOfferingId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public void setSourceUpdatedAt(LocalDateTime sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }
}