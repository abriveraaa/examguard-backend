package com.example.backend.repository.core;

import com.example.backend.entity.core.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAccessRepository extends JpaRepository<UserAccess, Long> {

    Optional<UserAccess> findByUsername(String username);

    List<UserAccess> findByUsernameIn(Collection<String> usernames);

    Optional<UserAccess> findBySchoolId(String schoolId);

    List<UserAccess> findByEligibleForReactivationTrue();

    List<UserAccess> findByEligibleForReactivationTrueAndRole(String role);

    List<UserAccess> findByIsActiveFalseOrIsBlockedTrue();
}