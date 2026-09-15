package com.pedidos360.db.repository;
import com.pedidos360.db.model.Venta;
import com.pedidos360.db.model.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByClienteId(Long clienteId);
    List<Venta> findByClienteIdOrderByFechaDescIdDesc(Long clienteId);
    Page<Venta> findAllByOrderByFechaDescIdDesc(Pageable pageable);
    Page<Venta> findByClienteIdOrderByFechaDescIdDesc(Long clienteId, Pageable pageable);
    Page<Venta> findByEstadoNotInOrderByFechaDescIdDesc(List<EstadoPedido> estados, Pageable pageable);
}
