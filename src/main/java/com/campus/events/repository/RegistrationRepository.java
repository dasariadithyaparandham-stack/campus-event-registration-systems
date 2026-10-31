package com.campus.events.repository;

import com.campus.events.model.EventRegistration;
import com.campus.events.model.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<EventRegistration, Long>, JpaSpecificationExecutor<EventRegistration> {

    Optional<EventRegistration> findByRegistrationReference(String registrationReference);

    Optional<EventRegistration> findByQrCodeToken(String qrCodeToken);

    List<EventRegistration> findByStudentIdOrderByRegisteredAtDesc(Long studentId);

    List<EventRegistration> findByStudentIdAndRegistrationStatusOrderByRegisteredAtDesc(Long studentId, RegistrationStatus status);

    Optional<EventRegistration> findByStudentIdAndEventId(Long studentId, Long eventId);

    Optional<EventRegistration> findByStudentIdAndEventIdAndRegistrationStatus(Long studentId, Long eventId, RegistrationStatus status);

    boolean existsByStudentIdAndEventIdAndRegistrationStatus(Long studentId, Long eventId, RegistrationStatus status);

    List<EventRegistration> findByEventId(Long eventId);

    List<EventRegistration> findByEventIdAndRegistrationStatus(Long eventId, RegistrationStatus status);

    Page<EventRegistration> findByEventId(Long eventId, Pageable pageable);

    Page<EventRegistration> findByEventIdAndRegistrationStatus(Long eventId, RegistrationStatus status, Pageable pageable);

    long countByRegistrationStatus(RegistrationStatus status);

    long countByEventIdAndRegistrationStatus(Long eventId, RegistrationStatus status);

    @Query("SELECT r FROM EventRegistration r JOIN FETCH r.student JOIN FETCH r.event ORDER BY r.registeredAt DESC")
    List<EventRegistration> findRecentRegistrations(Pageable pageable);

    @Query("SELECT r FROM EventRegistration r " +
           "JOIN FETCH r.student s " +
           "JOIN FETCH r.event e " +
           "LEFT JOIN FETCH r.attendance a " +
           "WHERE (:eventId IS NULL OR e.id = :eventId) " +
           "AND (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(r.registrationReference) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(s.studentIdNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR r.registrationStatus = :status)")
    Page<EventRegistration> searchParticipants(@Param("eventId") Long eventId,
                                              @Param("keyword") String keyword,
                                              @Param("status") RegistrationStatus status,
                                              Pageable pageable);

    @Query("SELECT r FROM EventRegistration r " +
           "JOIN FETCH r.student s " +
           "JOIN FETCH r.event e " +
           "LEFT JOIN FETCH r.attendance a " +
           "WHERE (:eventId IS NULL OR e.id = :eventId) " +
           "AND (:status IS NULL OR r.registrationStatus = :status) " +
           "ORDER BY r.registeredAt DESC")
    List<EventRegistration> findParticipantsForReport(@Param("eventId") Long eventId,
                                                      @Param("status") RegistrationStatus status);

    @Query("SELECT e.category, COUNT(r) FROM EventRegistration r JOIN r.event e " +
           "WHERE (:startDate IS NULL OR r.registeredAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.registeredAt <= :endDate) " +
           "GROUP BY e.category ORDER BY COUNT(r) DESC")
    List<Object[]> countRegistrationsByCategory(@Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT s.department, COUNT(r) FROM EventRegistration r JOIN r.student s " +
           "WHERE s.department IS NOT NULL " +
           "AND (:startDate IS NULL OR r.registeredAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.registeredAt <= :endDate) " +
           "GROUP BY s.department ORDER BY COUNT(r) DESC")
    List<Object[]> countRegistrationsByDepartment(@Param("startDate") java.time.LocalDateTime startDate,
                                                  @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM EventRegistration r " +
           "WHERE (:category IS NULL OR r.event.category = :category) " +
           "AND (:startDate IS NULL OR r.registeredAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.registeredAt <= :endDate)")
    long countFilteredRegistrations(@Param("category") String category,
                                    @Param("startDate") java.time.LocalDateTime startDate,
                                    @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM EventRegistration r " +
           "WHERE r.registrationStatus = :status " +
           "AND (:category IS NULL OR r.event.category = :category) " +
           "AND (:startDate IS NULL OR r.registeredAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.registeredAt <= :endDate)")
    long countFilteredByStatus(@Param("status") RegistrationStatus status,
                               @Param("category") String category,
                               @Param("startDate") java.time.LocalDateTime startDate,
                               @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Attendance a " +
           "WHERE a.attendanceStatus = 'PRESENT' " +
           "AND (:category IS NULL OR a.registration.event.category = :category) " +
           "AND (:startDate IS NULL OR a.registration.registeredAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.registration.registeredAt <= :endDate)")
    long countFilteredPresentAttendance(@Param("category") String category,
                                        @Param("startDate") java.time.LocalDateTime startDate,
                                        @Param("endDate") java.time.LocalDateTime endDate);
}

