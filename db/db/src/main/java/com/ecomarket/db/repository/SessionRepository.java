package com.ecomarket.db.repository;

import com.ecomarket.db.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByExpiresAtAfter(Instant now);
}
