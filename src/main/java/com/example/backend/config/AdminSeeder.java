package com.example.backend.config;

import com.example.backend.entity.core.AdminProfile;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.repository.core.UserAccessRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Profile("dev")
@Component
public class AdminSeeder implements CommandLineRunner {

    private final AdminProfileRepository adminProfileRepository;
    private final UserAccessRepository userAccessRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminProfileRepository adminProfileRepository,
                       UserAccessRepository userAccessRepository,
                       PasswordEncoder passwordEncoder) {
        this.adminProfileRepository = adminProfileRepository;
        this.userAccessRepository = userAccessRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String originalId = "2024-ADMIN-001";
        String username = "2024ADMIN001";

        if (userAccessRepository.findByUsername(username).isEmpty()) {
            UserAccess access = new UserAccess();
            access.setSchoolId(originalId);
            access.setUsername(username);
            access.setEmail("examguard.system@gmail.com");
            access.setPasswordHash(passwordEncoder.encode("Admin@123"));
            access.setRole("ADMIN");
            access.setActive(true);
            access.setBlocked(false);
            access.setMustChangePassword(false);
            access.setFailedLoginAttempts(0);
            userAccessRepository.save(access);
        }

        if (adminProfileRepository.findByEmployeeId(originalId).isEmpty()) {
            AdminProfile admin = new AdminProfile();
            admin.setEmployeeId(originalId);
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail("examguard.system@gmail.com");
            admin.setBirthDate(LocalDate.of(1997,5,23));
            admin.setIsActive(true);
            adminProfileRepository.save(admin);
        }
    }
}