package com.example.backend.service.cache;

import com.example.backend.service.student.StudentEvictCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrarCacheService {

    private final StudentEvictCacheService studentCacheService;
    private final FacultyCacheService facultyCacheService;

    public void evictAfterRegistrarSync() {
        studentCacheService.evictAllStudents();
        facultyCacheService.evictAllFaculty();
    }
}