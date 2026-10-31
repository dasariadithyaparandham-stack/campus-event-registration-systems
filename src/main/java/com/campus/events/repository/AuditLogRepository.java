package com.campus.events.repository;

import com.campus.events.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.actor ORDER BY a.timestamp DESC")
    Page<AuditLog> findAllWithActor(Pageable pageable);

    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.actor ORDER BY a.timestamp DESC")
    List<AuditLog> findTop10RecentLogs(Pageable pageable);
}

