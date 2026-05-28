package com.example.backend.controller.admin;

import com.example.backend.dto.admin.users.AdminUsersExportRequest;
import com.example.backend.dto.core.AdminUserResponse;
import com.example.backend.dto.student.dashboard.StudentUserResponse;
import com.example.backend.dto.core.FacultyUserResponse;
import com.example.backend.dto.core.UserDetailsResponse;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.core.AdminUserManagementService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@CrossOrigin
@AllArgsConstructor
public class UserManagementController {

    private final AdminUserManagementService service;
    private final AuthService authService;

    @GetMapping("/admins")
    public List<AdminUserResponse> getAdmins(
    ) {
        return service.getAdmins();
    }

    @GetMapping("/students")
    public List<StudentUserResponse> getStudents(

    ) {
        return service.getStudents();
    }

    @GetMapping("/faculty")
    public List<FacultyUserResponse> getFaculty(

    ) {
        return service.getFaculty();
    }

    @GetMapping("/details")
    public UserDetailsResponse getUserDetails(
            @RequestParam String schoolId,
            @RequestParam String role
    ) {
        return service.getUserDetails(schoolId, role);
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestBody AdminUsersExportRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = service.exportUsers(request, user);

        String format = request.getFormat() == null
                ? "PDF"
                : request.getFormat().trim().toUpperCase();

        boolean excel = "EXCEL".equals(format) || "XLSX".equals(format);

        String contentType = excel
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : MediaType.APPLICATION_PDF_VALUE;

        String extension = excel ? "xlsx" : "pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"examguard-users." + extension + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(bytes);
    }
}