package com.ecomarket.db.repository;
import com.ecomarket.db.model.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<CategoriaProducto, Long> {
    
}
