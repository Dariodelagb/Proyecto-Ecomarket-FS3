package com.ecomarket.db.repository;
import com.ecomarket.db.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    
}