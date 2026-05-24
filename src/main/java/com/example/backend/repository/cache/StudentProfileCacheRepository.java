package com.example.backend.repository.cache;

import com.example.backend.dto.faculty.students.FacultyStudentDTO;
import com.example.backend.entity.cache.StudentProfileCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
    SELECT DISTINCT new com.example.backend.dto.faculty.students.FacultyStudentDTO(
        s.studentId,
        CONCAT(s.firstName, ' ', s.lastName),
        s.emailAddress,

        s.collegeCode,
        s.collegeName,

        s.programCode,
        s.programName,
        s.yearLevel,
        s.sectionName,

        co.courseCode,
        co.courseDescription,
        co.classOfferingId
    )
    FROM StudentProfileCache s
    JOIN ClassEnrollmentCache ce
        ON ce.studentId = s.studentId
    JOIN ClassOfferingCache co
        ON co.classOfferingId = ce.classOfferingId
    WHERE ce.classOfferingId = :classOfferingId
    AND ce.status = 'ENROLLED'
    ORDER BY CONCAT(s.firstName, ' ', s.lastName)
""")
    List<FacultyStudentDTO> findStudentsByClassOffering(
            @Param("classOfferingId") String classOfferingId
    );

    @Query("""
    SELECT DISTINCT new com.example.backend.dto.faculty.students.FacultyStudentDTO(
        s.studentId,
        CONCAT(s.firstName, ' ', s.lastName),
        s.emailAddress,

        s.collegeCode,
        s.collegeName,

        s.programCode,
        s.programName,
        s.yearLevel,
        s.sectionName,

        co.courseCode,
        co.courseDescription,
        co.classOfferingId
    )
    FROM StudentProfileCache s
    JOIN ClassEnrollmentCache ce
        ON ce.studentId = s.studentId
    JOIN FacultyLoadCache fl
        ON fl.classOfferingId = ce.classOfferingId
    JOIN ClassOfferingCache co
        ON co.classOfferingId = ce.classOfferingId
    WHERE fl.employeeId = :employeeId
    AND fl.status = 'ACTIVE'
    AND co.academicYear = :academicYear
    AND co.term = :term
    AND ce.status = 'ENROLLED'
    ORDER BY CONCAT(s.firstName, ' ', s.lastName)
""")
    List<FacultyStudentDTO> findStudentsByFacultyPeriod(
            @Param("employeeId") String employeeId,
            @Param("academicYear") String academicYear,
            @Param("term") String term
    );

    @Query("""
        SELECT DISTINCT new com.example.backend.dto.faculty.students.FacultyStudentDTO(
            sp.studentId,
            CONCAT(sp.firstName, ' ', sp.lastName),
            sp.emailAddress,
        
            sp.collegeCode,
            sp.collegeName,
        
            sp.programCode,
            sp.programName,
            sp.yearLevel,
            sp.sectionName,
        
            co.courseCode,
            co.courseDescription,
            CAST(co.classOfferingId AS string)
        )
        FROM StudentProfileCache sp
        JOIN ClassEnrollmentCache ce
        ON ce.studentId = sp.studentId
        JOIN ClassOfferingCache co
        ON co.classOfferingId = ce.classOfferingId
        JOIN FacultyLoadCache fl
        ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :employeeId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND co.courseCode = :courseCode
        ORDER BY sp.studentId
        """)
    List<FacultyStudentDTO> findStudentsByFacultyCourse(
            String employeeId,
            String academicYear,
            String term,
            String courseCode
    );

    @Query("""
        SELECT DISTINCT new com.example.backend.dto.faculty.students.FacultyStudentDTO(
            sp.studentId,
            CONCAT(sp.firstName, ' ', sp.lastName),
            sp.emailAddress,
        
            sp.collegeCode,
            sp.collegeName,
        
            sp.programCode,
            sp.programName,
            sp.yearLevel,
            sp.sectionName,
        
            co.courseCode,
            co.courseDescription,
            CAST(co.classOfferingId AS string)
        )
        FROM StudentProfileCache sp
        JOIN ClassEnrollmentCache ce
        ON ce.studentId = sp.studentId
        JOIN ClassOfferingCache co
        ON co.classOfferingId = ce.classOfferingId
        JOIN FacultyLoadCache fl
        ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :employeeId
        AND co.classOfferingId = :classOfferingId
        ORDER BY sp.studentId
        """)
    List<FacultyStudentDTO> findStudentsByFacultySection(
            String employeeId,
            String classOfferingId
    );

    @Query("""
        SELECT DISTINCT new com.example.backend.dto.faculty.students.FacultyStudentDTO(
            sp.studentId,
            CONCAT(sp.firstName, ' ', sp.lastName),
            sp.emailAddress,
        
            sp.collegeCode,
            sp.collegeName,
        
            sp.programCode,
            sp.programName,
            sp.yearLevel,
            sp.sectionName,
        
            co.courseCode,
            co.courseDescription,
            CAST(co.classOfferingId AS string)
        )
        FROM StudentProfileCache sp
        JOIN ClassEnrollmentCache ce
        ON ce.studentId = sp.studentId
        JOIN ClassOfferingCache co
        ON co.classOfferingId = ce.classOfferingId
        JOIN FacultyLoadCache fl
        ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :employeeId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND sp.programCode = :programCode
        AND sp.yearLevel = :yearLevel
        AND sp.sectionName = :sectionName
        ORDER BY sp.studentId
        """)
    List<FacultyStudentDTO> findStudentsByFacultySectionGroup(
            String employeeId,
            String academicYear,
            String term,
            String programCode,
            String yearLevel,
            String sectionName
    );

    @Query("""
        SELECT DISTINCT new com.example.backend.dto.faculty.students.FacultyStudentDTO(
            sp.studentId,
            CONCAT(sp.firstName, ' ', sp.lastName),
            sp.emailAddress,
        
            sp.collegeCode,
            sp.collegeName,
        
            sp.programCode,
            sp.programName,
            sp.yearLevel,
            sp.sectionName,
        
            co.courseCode,
            co.courseDescription,
            CAST(co.classOfferingId AS string)
        )
        FROM StudentProfileCache sp
        JOIN ClassEnrollmentCache ce
        ON ce.studentId = sp.studentId
        JOIN ClassOfferingCache co
        ON co.classOfferingId = ce.classOfferingId
        JOIN FacultyLoadCache fl
        ON fl.classOfferingId = co.classOfferingId
        WHERE fl.employeeId = :employeeId
        AND co.academicYear = :academicYear
        AND co.term = :term
        AND co.courseCode = :courseCode
        AND sp.programCode = :programCode
        AND sp.yearLevel = :yearLevel
        AND sp.sectionName = :sectionName
        ORDER BY sp.studentId
        """)
    List<FacultyStudentDTO> findStudentsByFacultyCourseAndSectionGroup(
            String employeeId,
            String academicYear,
            String term,
            String courseCode,
            String programCode,
            String yearLevel,
            String sectionName
    );

}