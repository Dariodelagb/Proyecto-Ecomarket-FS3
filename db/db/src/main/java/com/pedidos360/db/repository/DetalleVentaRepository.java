package com.pedidos360.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pedidos360.db.model.DetalleVenta;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    boolean existsByProductoId(Long productoId);
}
