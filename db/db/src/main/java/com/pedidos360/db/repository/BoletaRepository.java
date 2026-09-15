package com.pedidos360.db.repository;
import com.pedidos360.db.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    
}