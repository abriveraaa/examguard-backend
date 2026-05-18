package com.example.backend.entity.cache;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_profile_cache")
public class FacultyProfileCache {

    @Id
    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "birth_date")
    private String birthDate;

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

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public void setSourceUpdatedAt(LocalDateTime sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }
}