package com.example.backend.repository.cache;

import com.example.backend.entity.cache.FacultyLoadCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacultyLoadCacheRepository extends JpaRepository<FacultyLoadCache, String> {
    List<FacultyLoadCache> findByEmployeeId(String employeeId);
    boolean existsByEmployeeIdAndClassOfferingIdAndStatusIgnoreCase(
            String employeeId,
            String classOfferingId,
            String status
    );
}