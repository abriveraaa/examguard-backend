package com.example.backend.dto.core;

public class LogoutRequest {

    private String sessionToken;

    public LogoutRequest() {
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}