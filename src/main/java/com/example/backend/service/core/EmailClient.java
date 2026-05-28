package com.example.backend.service.core;

public interface EmailClient {

    void sendHtmlEmail(String toEmail, String subject, String html);
}