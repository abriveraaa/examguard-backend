package com.example.backend.service.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Profile("prod")
public class BrevoEmailClient {

    private final WebClient webClient;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String fromEmail;

    public BrevoEmailClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    public void sendHtmlEmail(String toEmail, String subject, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", "ExamGuard System",
                        "email", fromEmail
                ),
                "to", List.of(
                        Map.of("email", toEmail)
                ),
                "subject", subject,
                "htmlContent", html
        );

        System.out.println("BREVO KEY LOADED: " + (apiKey != null && apiKey.startsWith("xkeysib-")));
        System.out.println("BREVO KEY LENGTH: " + (apiKey == null ? 0 : apiKey.length()));
        System.out.println("FROM EMAIL: " + fromEmail);

        webClient.post()
                .uri("/smtp/email")
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException(
                                        "Brevo failed: " + response.statusCode() + " | " + errorBody
                                ))
                )
                .bodyToMono(String.class)
                .block();
    }
}