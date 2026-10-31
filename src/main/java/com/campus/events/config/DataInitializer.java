package com.campus.events.config;

import com.campus.events.model.AppUser;
import com.campus.events.model.Role;
import com.campus.events.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            // 1. Ensure Default Admin Account
            Optional<AppUser> existingAdmin = userRepository.findByEmail("admin@campus.edu");
            if (existingAdmin.isEmpty()) {
                AppUser admin = new AppUser();
                admin.setFullName("Campus Administrator");
                admin.setEmail("admin@campus.edu");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                admin.setStudentIdNumber("ADM-001");
                admin.setDepartment("Academic Affairs");
                admin.setPhone("+91 9876543210");
                admin.setActive(true);
                userRepository.save(admin);
                log.info("Initialized default Administrator: admin@campus.edu");
            } else {
                // Ensure correct BCrypt hash
                AppUser admin = existingAdmin.get();
                if (!passwordEncoder.matches("Admin@123", admin.getPasswordHash())) {
                    admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                    userRepository.save(admin);
                    log.info("Updated Administrator password hash: admin@campus.edu");
                }
            }

            // 2. Ensure Sample Student Passwords Match "Student@123"
            String[] sampleStudentEmails = {"john.doe@campus.edu", "jane.smith@campus.edu", "raj.patel@campus.edu"};
            for (String email : sampleStudentEmails) {
                userRepository.findByEmail(email).ifPresent(student -> {
                    if (!passwordEncoder.matches("Student@123", student.getPasswordHash())) {
                        student.setPasswordHash(passwordEncoder.encode("Student@123"));
                        userRepository.save(student);
                        log.info("Synchronized password hash for student: {}", email);
                    }
                });
            }

            // 3. Ensure Sample Organizer Password Matches "Organizer@123"
            userRepository.findByEmail("organizer@campus.edu").ifPresent(organizer -> {
                if (!passwordEncoder.matches("Organizer@123", organizer.getPasswordHash())) {
                    organizer.setPasswordHash(passwordEncoder.encode("Organizer@123"));
                    userRepository.save(organizer);
                    log.info("Synchronized password hash for organizer: organizer@campus.edu");
                }
            });

        } catch (Exception e) {
            log.error("DataInitializer error: {}", e.getMessage(), e);
        }
    }
}

