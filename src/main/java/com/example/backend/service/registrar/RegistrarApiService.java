package com.example.backend.service.registrar;

import com.example.backend.audit.TrackActivity;
import com.example.backend.dto.registrar.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegistrarApiService {

    @Value("${registrar.api.base-url}")
    private String baseUrl;

    private final HttpClient httpClient;
    private final Gson gson;

    public RegistrarApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    // =========================
    // ENDPOINT METHODS
    // =========================

    @TrackActivity(
            module = "REGISTRAR_API",
            action = "FETCH_STUDENTS",
            message = "Registrar student data fetched"
    )
    public List<StudentDTO> fetchStudents() {
        return fetchList("student", new TypeToken<List<StudentDTO>>() {}.getType());
    }

    @TrackActivity(
            module = "REGISTRAR_API",
            action = "FETCH_FACULTY",
            message = "Registrar faculty data fetched"
    )
    public List<FacultyDTO> fetchFaculty() {
        return fetchList("faculty", new TypeToken<List<FacultyDTO>>() {}.getType());
    }

    @TrackActivity(
            module = "REGISTRAR_API",
            action = "FETCH_CLASS_OFFERINGS",
            message = "Registrar class offerings fetched"
    )
    public List<ClassOfferingDTO> fetchClassOfferings() {
        return fetchList("class_offering", new TypeToken<List<ClassOfferingDTO>>() {}.getType());
    }

    @TrackActivity(
            module = "REGISTRAR_API",
            action = "FETCH_CLASS_ENROLLMENTS",
            message = "Registrar class enrollments fetched"
    )
    public List<ClassEnrollmentDTO> fetchClassEnrollments() {
        return fetchList("class_enrollment",
                new TypeToken<List<ClassEnrollmentDTO>>() {}.getType());
    }

    @TrackActivity(
            module = "REGISTRAR_API",
            action = "FETCH_FACULTY_LOADS",
            message = "Registrar faculty loads fetched"
    )
    public List<FacultyLoadDTO> fetchFacultyLoads() {
        return fetchList("faculty_load",
                new TypeToken<List<FacultyLoadDTO>>() {}.getType());
    }


    // =========================
    // GENERIC FETCH METHOD
    // =========================
    private <T> List<T> fetchList(String endpoint, Type type) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }

            List<T> result = gson.fromJson(response.body(), type);

            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}