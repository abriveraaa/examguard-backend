package com.example.backend.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FacultyCacheService {

    private final CacheSupportService cache;

    public void evictFaculty(String employeeId) {
        cache.evict("facultyProfile", employeeId);
        cache.evict("facultyDashboard", employeeId);
        cache.evict("facultyById", employeeId);
    }

    public void evictAllFaculty() {
        cache.clear("facultyProfile");
        cache.clear("facultyDashboard");
        cache.clear("facultyById");
        cache.clear("allFaculty");
    }
}