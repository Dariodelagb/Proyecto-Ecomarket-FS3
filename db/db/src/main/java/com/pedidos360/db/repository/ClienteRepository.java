package com.pedidos360.db.repository;
import com.pedidos360.db.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByEmailAndRutAndDvrutAndContrasena(String email, Integer rut, String dvrut, String contrasena);
}
