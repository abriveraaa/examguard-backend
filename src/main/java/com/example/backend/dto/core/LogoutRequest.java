package com.example.backend.dto.core;

import java.io.Serializable;

public class LogoutRequest implements Serializable {

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