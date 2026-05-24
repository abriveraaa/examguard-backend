package com.example.backend.repository.cache;

import com.example.backend.dto.faculty.students.FacultyAcademicPeriodDTO;
import com.example.backend.dto.faculty.students.FacultyCourseDTO;
import com.example.backend.dto.faculty.students.FacultySectionDTO;
import com.example.backend.entity.cache.FacultyLoadCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacultyLoadCacheRepository extends JpaRepository<FacultyLoadCache, String> {
    List<FacultyLoadCache> findByEmployeeId(String employeeId);
    boolean existsByEmployeeIdAndClassOfferingIdAndStatusIgnoreCase(
            String employeeId,
            String classOfferingId,
            String status
    );

    boolean existsByEmployeeIdAndClassOfferingId(
            String employeeId,
            String classOfferingId
    );

    @Query("""
    SELECT DISTINCT new com.example.backend.dto.faculty.students.FacultyAcademicPeriodDTO(
        co.academicYear,
        co.term,
        co.status
    )
    FROM FacultyLoadCache fl
    JOIN ClassOfferingCache co
        ON co.classOfferingId = fl.classOfferingId
    WHERE fl.employeeId = :employeeId
    AND fl.status = 'ACTIVE'
    ORDER BY co.academicYear DESC, co.term DESC
""")
    List<FacultyAcademicPeriodDTO> findAcademicPeriodsByFaculty(
            @Param("employeeId") String employeeId
    );
}