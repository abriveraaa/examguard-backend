package com.example.backend.config;

import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.utility.SessionContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SessionContextFilter implements Filter {

    private final AuthService authService;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            String authorizationHeader = httpRequest.getHeader("Authorization");

            UserAccess currentUser = authService.getUserFromSession(authorizationHeader);

            if (currentUser != null) {
                SessionContext.set(
                        currentUser.getUsername(),
                        currentUser.getSchoolId(),
                        currentUser.getRole()
                );
            } else {
                String fallbackUserId = httpRequest.getHeader("X-User-Id");
                String fallbackRole = httpRequest.getHeader("X-Role");

                if (fallbackUserId != null && fallbackRole != null) {
                    SessionContext.set(
                            fallbackUserId,
                            fallbackUserId,
                            fallbackRole
                    );
                }
            }

            chain.doFilter(request, response);

        } finally {
            SessionContext.clear();
        }
    }
}