package com.example.backend.controller.admin;

import com.example.backend.dto.core.AdminUserResponse;
import com.example.backend.dto.core.CreateAdminProfileRequest;
import com.example.backend.dto.core.DeactivateRequest;
import com.example.backend.dto.core.UpdateAdminProfileRequest;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.core.AdminProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/profiles")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;
    private final AuthService authService;

    public AdminProfileController(AdminProfileService adminProfileService, AuthService authService) {
        this.adminProfileService = adminProfileService;
        this.authService = authService;
    }

    @PostMapping
    public String createAdminProfile(@RequestBody CreateAdminProfileRequest request) {
        return adminProfileService.createAdminProfile(request);
    }

    @GetMapping
    public List<AdminUserResponse> getAllAdminProfiles() {
        return adminProfileService.getAllAdminProfiles();
    }

    @GetMapping("/{employeeId}")
    public AdminUserResponse getAdminProfileByEmployeeId(@PathVariable String employeeId) {
        return adminProfileService.getAdminProfileByEmployeeId(employeeId);
    }

    @PutMapping("/{employeeId}")
    public String updateAdminProfile(@PathVariable String employeeId,
                                     @RequestBody UpdateAdminProfileRequest request) {
        return adminProfileService.updateAdminProfile(employeeId, request);
    }

    @PatchMapping("/{employeeId}/deactivate")
    public String deactivateAdminProfile(@PathVariable String employeeId,
                                         @RequestBody DeactivateRequest request) {
        return adminProfileService.deactivateAdminProfile(
                employeeId,
                request.getCurrentAdminId(),
                request.getReason()
        );
    }

    @PatchMapping("/{employeeId}/reactivate")
    public String reactivateAdminProfile(
            @PathVariable String employeeId,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {



        UserAccess admin = null;


        if (token != null && !token.isBlank()) {
            admin = authService.getUserFromSession(token);
        }

        return adminProfileService.reactivateAdminProfile(employeeId, admin);
    }

}