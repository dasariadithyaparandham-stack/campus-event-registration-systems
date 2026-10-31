package com.campus.events.service;

import com.campus.events.dto.UserProfileDto;
import com.campus.events.dto.UserRegistrationDto;
import com.campus.events.exception.RegistrationException;
import com.campus.events.exception.ResourceNotFoundException;
import com.campus.events.model.AppUser;
import com.campus.events.model.Role;
import com.campus.events.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public AppUser registerStudent(UserRegistrationDto dto) {
        if (!dto.isPasswordMatching()) {
            throw new RegistrationException("Passwords do not match");
        }
        if (userRepository.existsByEmail(dto.getEmail().toLowerCase().trim())) {
            throw new RegistrationException("An account with email " + dto.getEmail() + " already exists");
        }

        AppUser user = new AppUser();
        user.setFullName(dto.getFullName().trim());
        user.setEmail(dto.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        // Security rule: public registration strictly assigns STUDENT role
        user.setRole(Role.STUDENT);
        user.setStudentIdNumber(dto.getStudentIdNumber() != null ? dto.getStudentIdNumber().trim() : null);
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setDepartment(dto.getDepartment() != null ? dto.getDepartment().trim() : null);
        user.setActive(true);

        AppUser savedUser = userRepository.save(user);
        auditService.log(savedUser, "STUDENT_SIGNUP", "AppUser", savedUser.getId(), "Student registered account: " + savedUser.getEmail());
        return savedUser;
    }

    @Transactional
    public AppUser updateProfile(Long userId, UserProfileDto dto) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setFullName(dto.getFullName().trim());
        user.setStudentIdNumber(dto.getStudentIdNumber() != null ? dto.getStudentIdNumber().trim() : null);
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setDepartment(dto.getDepartment() != null ? dto.getDepartment().trim() : null);

        AppUser updated = userRepository.save(user);
        auditService.log(updated, "PROFILE_UPDATED", "AppUser", updated.getId(), "User updated profile details");
        return updated;
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    @Transactional(readOnly = true)
    public AppUser getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}

