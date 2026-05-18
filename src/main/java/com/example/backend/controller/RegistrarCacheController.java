package com.example.backend.controller;

import com.example.backend.dto.core.ReactivateUserRequest;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.core.RegistrarAccessSyncService;
import com.example.backend.service.core.ReactivationService;
import com.example.backend.service.registrar.RegistrarSyncService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/cache")
public class RegistrarCacheController {

    private final RegistrarSyncService registrarSyncService;
    private final RegistrarAccessSyncService registrarAccessSyncService;
    private final ReactivationService reactivationService;
    private final AuthService authService;

    public RegistrarCacheController(RegistrarSyncService registrarSyncService,
                                    RegistrarAccessSyncService registrarAccessSyncService,
                                    ReactivationService reactivationService,
                                    AuthService authService) {
        this.registrarSyncService = registrarSyncService;
        this.registrarAccessSyncService = registrarAccessSyncService;
        this.reactivationService = reactivationService;
        this.authService = authService;
    }

    @PostMapping("/refresh-registrar")
    public String refreshRegistrarCache(
            @RequestHeader("Authorization") String token
    ) {
        UserAccess admin = authService.getUserFromSession(token);

        if (admin == null) {
            return "Session user not found. Please login again.";
        }

        return registrarSyncService.initialSync(admin);
    }

    @PostMapping("/refresh-and-sync")
    public String refreshAndSyncAccess(
            @RequestHeader("Authorization") String token
    ) {
        UserAccess admin = authService.getUserFromSession(token);

        if (admin == null) {
            return "Session user not found. Please login again.";
        }

        registrarSyncService.initialSync(admin);

        return registrarAccessSyncService.refreshAndSyncAccessStatus();
    }

    @GetMapping("/eligible-reactivation")
    public List<UserAccess> getEligibleForReactivationUsers() {
        return registrarAccessSyncService.getEligibleForReactivationUsers();
    }

    @PostMapping("/bulk-reactivate")
    public String bulkReactivate(@RequestBody ReactivateUserRequest request) {
        return reactivationService.bulkReactivateEligibleUsers(request.getJustification());
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