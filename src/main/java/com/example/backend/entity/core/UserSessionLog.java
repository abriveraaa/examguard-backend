package com.example.backend.entity.core;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_session_log")
public class UserSessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_log_id")
    private Long sessionLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_id")
    private UserAccess userAccess;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "username")
    private String username;

    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    @Column(name = "login_status", nullable = false)
    private String loginStatus;

    @Column(name = "message")
    private String message;

    @Column(name = "login_at")
    private OffsetDateTime loginAt;

    @Column(name = "logout_at")
    private OffsetDateTime logoutAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UserSessionLog() {
    }

    public Long getSessionLogId() {
        return sessionLogId;
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

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(String loginStatus) {
        this.loginStatus = loginStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(OffsetDateTime loginAt) {
        this.loginAt = loginAt;
    }

    public OffsetDateTime getLogoutAt() {
        return logoutAt;
    }

    public void setLogoutAt(OffsetDateTime logoutAt) {
        this.logoutAt = logoutAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
}