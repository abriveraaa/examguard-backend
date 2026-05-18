package com.example.backend.service.registrar;

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
                System.out.println("API ERROR [" + endpoint + "]: " + response.body());
                return new ArrayList<>();
            }

            List<T> result = gson.fromJson(response.body(), type);

            System.out.println("Fetched " + endpoint + ": " + (result != null ? result.size() : 0));

            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("API EXCEPTION [" + endpoint + "]");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // =========================
    // ENDPOINT METHODS
    // =========================

    public List<StudentDTO> fetchStudents() {
        return fetchList("student", new TypeToken<List<StudentDTO>>() {}.getType());
    }

    public List<FacultyDTO> fetchFaculty() {
        return fetchList("faculty", new TypeToken<List<FacultyDTO>>() {}.getType());
    }

    public List<ClassOfferingDTO> fetchClassOfferings() {
        return fetchList("class_offering", new TypeToken<List<ClassOfferingDTO>>() {}.getType());
    }

    public List<ClassEnrollmentDTO> fetchClassEnrollments() {
        return fetchList("class_enrollment",
                new TypeToken<List<ClassEnrollmentDTO>>() {}.getType());
    }

    public List<FacultyLoadDTO> fetchFacultyLoads() {
        return fetchList("faculty_load",
                new TypeToken<List<FacultyLoadDTO>>() {}.getType());
    }
}