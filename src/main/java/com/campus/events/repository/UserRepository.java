package com.campus.events.repository;

import com.campus.events.model.AppUser;
import com.campus.events.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByActiveTrue();

    Page<AppUser> findByRole(Role role, Pageable pageable);
}

