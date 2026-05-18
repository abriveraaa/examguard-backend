package com.example.backend.controller.admin;

import com.example.backend.dto.core.AdminUserResponse;
import com.example.backend.dto.student.dashboard.StudentUserResponse;
import com.example.backend.dto.core.FacultyUserResponse;
import com.example.backend.dto.core.UserDetailsResponse;
import com.example.backend.service.core.AdminUserManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@CrossOrigin // allow JavaFX to call backend
public class UserManagementController {

    private final AdminUserManagementService service;

    public UserManagementController(AdminUserManagementService service) {
        this.service = service;
    }

    @GetMapping("/admins")
    public List<AdminUserResponse> getAdmins() {
        return service.getAdmins();
    }

    @GetMapping("/students")
    public List<StudentUserResponse> getStudents() {
        return service.getStudents();
    }

    @GetMapping("/faculty")
    public List<FacultyUserResponse> getFaculty() {
        return service.getFaculty();
    }

    @GetMapping("/details")
    public UserDetailsResponse getUserDetails(
            @RequestParam String schoolId,
            @RequestParam String role
    ) {
        return service.getUserDetails(schoolId, role);
    }
}