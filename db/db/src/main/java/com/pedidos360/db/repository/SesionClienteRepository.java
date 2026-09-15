package com.pedidos360.db.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pedidos360.db.model.SesionCliente;

public interface SesionClienteRepository extends JpaRepository<SesionCliente, Long> {
    Optional<SesionCliente> findByToken(String token);
    void deleteByToken(String token);

    @Query("select distinct s.cliente from SesionCliente s")
    List<com.pedidos360.db.model.Cliente> findClientesActivos();
}
