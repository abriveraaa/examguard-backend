package com.example.backend.service.student;

import com.example.backend.service.cache.CacheSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentEvictCacheService {

    private final CacheSupportService cache;

    public void evictStudent(String schoolId) {
        cache.evict("profileMe", schoolId);
        cache.evict("studentDashboard", schoolId);
        cache.evict("studentExams", schoolId);
        cache.evict("studentById", schoolId);
    }

    public void evictAllStudents() {
        cache.clear("profileMe");
        cache.clear("studentDashboard");
        cache.clear("studentExams");
        cache.clear("studentById");
        cache.clear("allStudents");
    }
}