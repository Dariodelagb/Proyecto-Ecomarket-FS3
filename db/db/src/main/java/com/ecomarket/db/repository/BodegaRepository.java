package com.ecomarket.db.repository;
import com.ecomarket.db.model.Bodega;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BodegaRepository extends JpaRepository<Bodega, Long> {
    Optional<Bodega> findByProductoId(Long productoId);
}
