package com.example.backend.service.cache;

import com.example.backend.service.student.ExamTakingCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamCacheService {

    private final CacheSupportService cache;
    private final ExamTakingCacheService examTakingCacheService;

    public void warmExamTaking(Long examId) {
        examTakingCacheService.warmCache(examId);
    }

    public void evictExamTaking(Long examId) {
        examTakingCacheService.evictCache(examId);
    }

    public void evictAdminMonitoringSummary() {
        cache.clear("adminMonitoringSummary");
    }
}