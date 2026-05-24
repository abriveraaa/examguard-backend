package com.example.backend.controller.faculty;

import com.example.backend.dto.faculty.students.FacultyAcademicPeriodDTO;
import com.example.backend.dto.faculty.students.FacultyStudentDTO;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.faculty.FacultyStudentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faculty/students")
@RequiredArgsConstructor
public class FacultyStudentsController {

    private final FacultyStudentsService facultyStudentsService;
    private final AuthService authService;

    @GetMapping("/academic-periods")
    public List<FacultyAcademicPeriodDTO> getAcademicPeriods(
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyStudentsService.getAcademicPeriods(
                user.getSchoolId()
        );
    }

    @GetMapping("/period")
    public List<FacultyStudentDTO> getStudentsByPeriod(
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestHeader("Authorization") String authorization
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        return facultyStudentsService.getStudentsByPeriod(
                user.getSchoolId(),
                academicYear,
                term
        );
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStudentsRoster(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String programCode,
            @RequestParam(required = false) String yearLevel,
            @RequestParam(required = false) String sectionName,
            @RequestParam(defaultValue = "pdf") String type
    ) {
        UserAccess user = authService.getUserFromSession(authorization);

        byte[] fileBytes =
                facultyStudentsService.exportStudentsRoster(
                        user.getSchoolId(),
                        academicYear,
                        term,
                        courseCode,
                        programCode,
                        yearLevel,
                        sectionName,
                        type
                );

        String extension =
                "excel".equalsIgnoreCase(type)
                        ? "xlsx"
                        : "pdf";

        MediaType mediaType =
                "excel".equalsIgnoreCase(type)
                        ? MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
                        : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"student-roster." + extension + "\""
                )
                .body(fileBytes);
    }
}