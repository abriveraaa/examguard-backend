package com.example.backend.repository.core;


import com.example.backend.entity.core.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, String>{
    Optional<AdminProfile> findByEmployeeId(String schoolId);
    List<AdminProfile> findByEmployeeIdIn(Collection<String> employeeIds);
    Optional<AdminProfile> findByEmail(String email);
    long countByIsActiveTrue();
}