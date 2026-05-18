package com.example.backend.repository.cache;

import com.example.backend.entity.cache.FacultyProfileCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FacultyProfileCacheRepository extends JpaRepository<FacultyProfileCache, String> {

    Optional<FacultyProfileCache> findByEmployeeId(String employeeId);
    List<FacultyProfileCache> findByEmployeeIdIn(Collection<String> employeeIds);
    Optional<FacultyProfileCache> findByEmployeeIdAndEmailAddressAndBirthDate(
            String employeeId,
            String emailAddress,
            String birthDate
    );
}