package com.example.backend.service.student;

import com.example.backend.service.cache.CacheSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentEvictCacheService {

    private final CacheSupportService cache;

    public void evictStudent(String schoolId) {
        cache.evict("studentDashboardProfile", schoolId);
        cache.evict("studentDashboardUpcomingExams", schoolId);
        cache.evict("studentDashboardResults", schoolId);
        cache.evict("studentDashboardViolations", schoolId);
        cache.evict("studentExamsRaw", schoolId);

        // optional old names, safe to keep during transition
        cache.evict("profileMe", schoolId);
        cache.evict("studentDashboard", schoolId);
        cache.evict("studentExams", schoolId);
        cache.evict("studentById", schoolId);
    }

    public void evictAllStudents() {
        cache.clear("studentDashboardProfile");
        cache.clear("studentDashboardUpcomingExams");
        cache.clear("studentDashboardResults");
        cache.clear("studentDashboardViolations");
        cache.clear("studentExamsRaw");

        cache.clear("profileMe");
        cache.clear("studentDashboard");
        cache.clear("studentExams");
        cache.clear("studentById");
        cache.clear("allStudents");
    }
}