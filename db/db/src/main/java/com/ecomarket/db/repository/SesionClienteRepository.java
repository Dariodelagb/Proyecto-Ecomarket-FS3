package com.ecomarket.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecomarket.db.model.SesionCliente;

public interface SesionClienteRepository extends JpaRepository<SesionCliente, Long> {
    Optional<SesionCliente> findByToken(String token);
    void deleteByToken(String token);
}
