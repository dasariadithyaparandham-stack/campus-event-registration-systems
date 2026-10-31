package com.campus.events.repository;

import com.campus.events.model.CampusEvent;
import com.campus.events.model.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<CampusEvent, Long>, JpaSpecificationExecutor<CampusEvent> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM CampusEvent e WHERE e.id = :id")
    Optional<CampusEvent> findByIdForUpdate(@Param("id") Long id);

    List<CampusEvent> findByStatus(EventStatus status);

    List<CampusEvent> findByStatusOrderByStartDatetimeAsc(EventStatus status);

    @Query("SELECT e FROM CampusEvent e WHERE e.status = :status AND e.registrationDeadline > :now ORDER BY e.startDatetime ASC")
    List<CampusEvent> findOpenUpcomingEvents(@Param("status") EventStatus status, @Param("now") LocalDateTime now);

    long countByStatus(EventStatus status);

    List<CampusEvent> findTop5ByOrderByRegisteredCountDesc();

    @Query("SELECT DISTINCT e.category FROM CampusEvent e ORDER BY e.category ASC")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT e.venue FROM CampusEvent e ORDER BY e.venue ASC")
    List<String> findAllVenues();

    List<CampusEvent> findByOrganizerIdOrderByStartDatetimeDesc(Long organizerId);

    long countByOrganizerId(Long organizerId);

    @Query("SELECT COALESCE(SUM(e.capacity), 0) FROM CampusEvent e WHERE (:categoryId IS NULL OR e.category = :categoryId)")
    long sumCapacityByCategory(@Param("categoryId") String categoryId);
}

