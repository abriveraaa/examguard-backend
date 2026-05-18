package com.example.backend.repository.cache;

import com.example.backend.entity.cache.ClassEnrollmentCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassEnrollmentCacheRepository extends JpaRepository<ClassEnrollmentCache, String> {
    List<ClassEnrollmentCache> findByStudentId(String studentId);
    boolean existsByStudentIdAndClassOfferingIdAndStatus(
            String studentId,
            String classOfferingId,
            String status
    );

}