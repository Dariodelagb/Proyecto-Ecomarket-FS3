package com.ecomarket.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecomarket.db.model.Contacto;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {
}
