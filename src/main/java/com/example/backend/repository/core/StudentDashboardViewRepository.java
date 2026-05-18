package com.example.backend.repository.core;

import com.example.backend.entity.core.StudentDashboardView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentDashboardViewRepository
        extends JpaRepository<StudentDashboardView, Long> {

    boolean existsByStudentIdAndItemTypeAndItemId(
            String studentId,
            String itemType,
            Long itemId
    );
}
