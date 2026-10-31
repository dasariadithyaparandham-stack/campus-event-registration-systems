package com.campus.events.repository;

import com.campus.events.model.Attendance;
import com.campus.events.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByRegistrationId(Long registrationId);

    long countByAttendanceStatus(AttendanceStatus status);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.registration.event.id = :eventId AND a.attendanceStatus = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") AttendanceStatus status);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.registration.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);
}

