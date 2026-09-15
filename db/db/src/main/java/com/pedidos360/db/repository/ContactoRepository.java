package com.pedidos360.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pedidos360.db.model.Contacto;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {
}
