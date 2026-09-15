package com.pedidos360.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pedidos360.db.model.RegistroAuditoria;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
}
