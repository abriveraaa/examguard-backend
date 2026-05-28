package com.example.backend.controller;

import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.LoginResult;
import com.example.backend.dto.core.*;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.utility.SessionContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/test")
    public String test() {
        return "Auth controller is working";
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse> activate(@RequestBody ActivateAccountRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        ActivationResult result = authService.activateAccount(
                request.getSchoolId(),
                request.getEmail(),
                request.getBirthday(),
                ipAddress
        );

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, result.getMessage())
            );
        }

        return ResponseEntity.ok(
                new ApiResponse(true, result.getMessage())
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);

        LoginResult result = authService.login(request.getUsername(), request.getPassword(), ipAddress);

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, result.getMessage()));
        }

        return ResponseEntity.ok(
                new ApiResponse(
                        true,
                        result.getMessage(),
                        result.getUsername(),
                        result.getSchoolId(),
                        result.getRole(),
                        result.getFirstName(),
                        result.getLastName(),
                        result.isMustChangePassword(),
                        result.getSessionToken()
                )
        );
    }
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        boolean success = authService.changePassword(request, ipAddress);

        if (!success) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false,
                            "Password change failed. Check your current password and new password rules.")
            );
        }

        return ResponseEntity.ok(
                new ApiResponse(true, "Password changed successfully.")
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        boolean success = authService.forgotPassword(request, ipAddress);

        if (!success) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Verification failed or account not found.")
            );
        }

        return ResponseEntity.ok(
                new ApiResponse(true, "A new temporary password has been sent to your email.")
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        boolean success = authService.logout(request, ipAddress);

        if (!success) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false,
                            "Logout failed. Invalid session token.",
                            null,
                            false)
            );
        }

        return ResponseEntity.ok(
                new ApiResponse(true,
                        "Logout successful.",
                        null,
                        false)
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}