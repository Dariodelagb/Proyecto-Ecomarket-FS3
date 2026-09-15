package com.pedidos360.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pedidos360.db.model.Carrito;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    Optional<Carrito> findByClienteId(Long clienteId);
}
