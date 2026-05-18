package com.example.backend.config;

import com.example.backend.utility.SessionContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SessionContextFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        try {
            String userId = httpRequest.getHeader("X-User-Id");
            String role = httpRequest.getHeader("X-Role");

            SessionContext.set(userId, role);

            chain.doFilter(request, response);

        } finally {
            SessionContext.clear();
        }
    }
}