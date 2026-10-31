package com.campus.events.repository;

import com.campus.events.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateCode(String certificateCode);

    Optional<Certificate> findByRegistrationId(Long registrationId);

    boolean existsByRegistrationId(Long registrationId);

    List<Certificate> findByStudentIdOrderByIssuedAtDesc(Long studentId);

    List<Certificate> findByEventIdOrderByIssuedAtDesc(Long eventId);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.event JOIN FETCH c.student WHERE c.certificateCode = :code")
    Optional<Certificate> findByCodeWithDetails(@Param("code") String code);

    long countByEventId(Long eventId);
}

