package com.example.backend.controller;

import com.example.backend.dto.core.ReactivateUserRequest;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.core.RegistrarSyncLogRepository;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.cache.RegistrarCacheService;
import com.example.backend.service.core.ReactivationService;
import com.example.backend.service.registrar.RegistrarSyncService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/admin/registrar-sync")
public class RegistrarSyncController {

    private final RegistrarSyncLogRepository registrarSyncLogRepository;
    private final RegistrarSyncService registrarSyncService;
    private final ReactivationService reactivationService;
    private final AuthService authService;

    @PostMapping("/sync")
    public String initialSync(@RequestHeader("Authorization") String token) {
        UserAccess admin = authService.getUserFromSession(token);

        if (admin == null) {
            return "Session user not found. Please login again.";
        }

        return registrarSyncService.initialSync(admin);
    }

    @GetMapping("/last-sync")
    public String getLastSuccessfulSync() {
        return registrarSyncLogRepository
                .findTopByStatusOrderByFinishedAtDesc("SUCCESS")
                .map(log -> log.getFinishedAt().toString())
                .orElse("");
    }

    @PostMapping("/reactivate-user")
    public String reactivateSingleUser(
            @RequestBody ReactivateUserRequest request,
            @RequestHeader("Authorization") String token
    ) {
        UserAccess admin = authService.getUserFromSession(token);

        return reactivationService.reactivateSingleUser(
                request.getSchoolId(),
                request.getRole(),
                request.getJustification(),
                admin
        );
    }
}