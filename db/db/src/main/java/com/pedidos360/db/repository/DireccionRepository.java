package com.pedidos360.db.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pedidos360.db.model.Direccion;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    List<Direccion> findByClienteId(Long clienteId);
}
