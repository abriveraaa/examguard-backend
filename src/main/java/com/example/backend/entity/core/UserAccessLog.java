package com.example.backend.entity.core;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_access_log")
public class UserAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_id")
    private UserAccess userAccess;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "username")
    private String username;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_status", nullable = false)
    private String eventStatus;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "ip_address")
    private String ipAddress;

    public UserAccessLog() {
    }

    public Long getLogId() {
        return logId;
    }

    public UserAccess getUserAccess() {
        return userAccess;
    }

    public void setUserAccess(UserAccess userAccess) {
        this.userAccess = userAccess;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}