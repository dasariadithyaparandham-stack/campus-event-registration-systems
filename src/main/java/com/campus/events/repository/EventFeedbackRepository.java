package com.campus.events.repository;

import com.campus.events.model.EventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {

    List<EventFeedback> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<EventFeedback> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<EventFeedback> findByRegistrationId(Long registrationId);

    boolean existsByRegistrationId(Long registrationId);

    boolean existsByStudentIdAndEventId(Long studentId, Long eventId);

    @Query("SELECT AVG(f.rating) FROM EventFeedback f WHERE f.event.id = :eventId")
    Double getAverageRatingByEventId(@Param("eventId") Long eventId);

    long countByEventId(Long eventId);
}

