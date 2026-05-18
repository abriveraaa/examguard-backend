package com.example.backend.repository.cache;

import com.example.backend.dto.core.CurrentTermDTO;
import com.example.backend.entity.cache.ClassOfferingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassOfferingCacheRepository extends JpaRepository<ClassOfferingCache, String> {

    List<ClassOfferingCache> findByAcademicYearAndTerm(String academicYear, String term);

    List<ClassOfferingCache> findByClassOfferingIdIn(List<String> classOfferingIds);

    @Query("""
            SELECT new com.example.backend.dto.core.CurrentTermDTO(
                t.academicYear,
                t.term
            )
            FROM ClassOfferingCache t
            WHERE t.status = 'ACTIVE'
        """)
    List<CurrentTermDTO> findCurrentTerm();

}