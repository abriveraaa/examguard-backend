package com.example.backend.repository.cache;

import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.exam.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentProfileCacheRepository extends JpaRepository<StudentProfileCache, String> {

    Optional<StudentProfileCache> findByStudentIdAndEmailAddressAndBirthDate(
            String studentId,
            String emailAddress,
            String birthDate
    );

    Optional<StudentProfileCache> findByStudentId(String schoolId);

    @Query("""
        SELECT DISTINCT s
        FROM StudentProfileCache s
        JOIN ClassEnrollmentCache ce
            ON ce.studentId = s.studentId
        WHERE ce.classOfferingId = :classOfferingId
        AND ce.status = 'ENROLLED'
    """)
    List<StudentProfileCache> findEnrolledStudentsByClassOfferingId(
            @Param("classOfferingId") String classOfferingId
    );

}