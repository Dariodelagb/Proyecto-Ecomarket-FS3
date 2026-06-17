package com.ecomarket.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecomarket.db.model.StockProducto;

public interface StockProductoRepository extends JpaRepository<StockProducto, Long> {
    Optional<StockProducto> findByProductoId(Long productoId);
}
