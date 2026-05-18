package com.example.backend.entity.cache;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_load_cache")
public class FacultyLoadCache {

    @Id
    @Column(name = "faculty_load_id")
    private String facultyLoadId;

    @Column(name = "employee_id")
    private String employeeId;

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

    public String getFacultyLoadId() { return facultyLoadId; }
    public void setFacultyLoadId(String facultyLoadId) { this.facultyLoadId = facultyLoadId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getClassOfferingId() { return classOfferingId; }
    public void setClassOfferingId(String classOfferingId) { this.classOfferingId = classOfferingId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public void setSourceUpdatedAt(LocalDateTime sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }
}