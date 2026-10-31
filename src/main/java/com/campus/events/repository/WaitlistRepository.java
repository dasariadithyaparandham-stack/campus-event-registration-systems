package com.campus.events.repository;

import com.campus.events.model.WaitlistEntry;
import com.campus.events.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByEventIdAndStudentId(Long eventId, Long studentId);

    Optional<WaitlistEntry> findByEventIdAndStudentIdAndStatusIn(Long eventId, Long studentId, Collection<WaitlistStatus> statuses);

    boolean existsByEventIdAndStudentIdAndStatusIn(Long eventId, Long studentId, Collection<WaitlistStatus> statuses);

    List<WaitlistEntry> findByStudentIdOrderByJoinedAtDesc(Long studentId);

    List<WaitlistEntry> findByStudentIdAndStatusInOrderByJoinedAtDesc(Long studentId, Collection<WaitlistStatus> statuses);

    List<WaitlistEntry> findByEventIdOrderByQueuePositionAsc(Long eventId);

    List<WaitlistEntry> findByEventIdAndStatusOrderByQueuePositionAsc(Long eventId, WaitlistStatus status);

    Optional<WaitlistEntry> findFirstByEventIdAndStatusOrderByQueuePositionAsc(Long eventId, WaitlistStatus status);

    long countByEventIdAndStatus(Long eventId, WaitlistStatus status);

    @Query("SELECT COALESCE(MAX(w.queuePosition), 0) FROM WaitlistEntry w WHERE w.event.id = :eventId")
    int findMaxQueuePositionByEventId(@Param("eventId") Long eventId);

    List<WaitlistEntry> findByStatusAndOfferExpiresAtBefore(WaitlistStatus status, LocalDateTime now);
}

