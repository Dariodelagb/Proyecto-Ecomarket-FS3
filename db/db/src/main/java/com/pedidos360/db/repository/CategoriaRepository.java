package com.pedidos360.db.repository;
import com.pedidos360.db.model.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<CategoriaProducto, Long> {
    
}
