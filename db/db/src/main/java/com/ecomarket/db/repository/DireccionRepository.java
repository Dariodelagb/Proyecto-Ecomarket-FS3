package com.ecomarket.db.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecomarket.db.model.Direccion;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    List<Direccion> findByClienteId(Long clienteId);
}
