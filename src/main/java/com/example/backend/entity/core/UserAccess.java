package com.example.backend.entity.core;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_access")
public class UserAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @Column(name = "school_id", nullable = false, unique = true)
    private String schoolId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @Column(name = "temp_password_sent_at")
    private OffsetDateTime tempPasswordSentAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "temp_password_expiry")
    private OffsetDateTime tempPasswordExpiry;

    @Column(name = "deactivation_reason")
    private String deactivationReason;

    @Column(name = "eligible_for_reactivation")
    private Boolean eligibleForReactivation = false;

    public UserAccess() {
    }

    public Long getAccessId() {
        return accessId;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() { return email; }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public OffsetDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(OffsetDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public OffsetDateTime getTempPasswordSentAt() {
        return tempPasswordSentAt;
    }

    public void setTempPasswordSentAt(OffsetDateTime tempPasswordSentAt) {
        this.tempPasswordSentAt = tempPasswordSentAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public OffsetDateTime getTempPasswordExpiry() { return tempPasswordExpiry; }

    public void setTempPasswordExpiry(OffsetDateTime tempPasswordExpiry) { this.tempPasswordExpiry = tempPasswordExpiry; }

    public String getDeactivationReason() {
        return deactivationReason;
    }

    public void setDeactivationReason(String deactivationReason) {
        this.deactivationReason = deactivationReason;
    }

    public Boolean getEligibleForReactivation() {
        return eligibleForReactivation;
    }

    public void setEligibleForReactivation(Boolean eligibleForReactivation) {
        this.eligibleForReactivation = eligibleForReactivation;
    }
}