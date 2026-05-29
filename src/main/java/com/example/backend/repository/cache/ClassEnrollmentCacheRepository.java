package com.example.backend.repository.cache;

import com.example.backend.entity.cache.ClassEnrollmentCache;
import com.example.backend.entity.cache.ClassOfferingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface ClassEnrollmentCacheRepository extends JpaRepository<ClassEnrollmentCache, String> {
    List<ClassEnrollmentCache> findByStudentId(String studentId);

    @Query("""
    SELECT DISTINCT ce.studentId
    FROM ClassEnrollmentCache ce
    WHERE ce.classOfferingId IN :classOfferingIds
""")
    List<String> findStudentIdsByClassOfferingIds(
            @Param("classOfferingIds") List<String> classOfferingIds
    );

    @Query("""
    SELECT co
    FROM ClassEnrollmentCache ce
    JOIN ClassOfferingCache co
        ON co.classOfferingId = ce.classOfferingId
    WHERE ce.studentId = :studentId
      AND ce.status='ENROLLED'
      AND co.status='ACTIVE'
""")
    List<ClassOfferingCache> findActiveOfferingsByStudentId(
            @Param("studentId") String studentId
    );

    @Query("""
    SELECT COUNT(DISTINCT enrollment.studentId)
    FROM ClassEnrollmentCache enrollment
    WHERE enrollment.status = 'ENROLLED'
      AND enrollment.classOfferingId IN (
          SELECT DISTINCT fl.classOfferingId
          FROM FacultyLoadCache fl
          WHERE fl.employeeId = :employeeId AND fl.status = 'ACTIVE'
      )
""")
    long countDistinctStudentsForFacultyDashboard(@Param("employeeId") String employeeId);

    @Query("""
    SELECT COUNT(DISTINCT ce.studentId)
    FROM ClassEnrollmentCache ce
    WHERE ce.classOfferingId IN :classOfferingIds
      AND ce.status = 'ENROLLED'
""")
    int countDistinctEnrolledStudentsByClassOfferingIds(
            @Param("classOfferingIds") Collection<String> classOfferingIds
    );
}