package com.pedidos360.db.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pedidos360.db.model.Cliente;
import com.pedidos360.db.model.DetalleVenta;
import com.pedidos360.db.model.EstadoPedido;
import com.pedidos360.db.model.RegistroAuditoria;
import com.pedidos360.db.model.SesionCliente;
import com.pedidos360.db.model.StockProducto;
import com.pedidos360.db.model.Venta;
import com.pedidos360.db.repository.RegistroAuditoriaRepository;
import com.pedidos360.db.repository.SesionClienteRepository;
import com.pedidos360.db.repository.StockProductoRepository;
import com.pedidos360.db.repository.VentaRepository;

@Service
public class PedidoService {
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
        EstadoPedido.CREADO, EnumSet.of(EstadoPedido.ACEPTADO, EstadoPedido.CANCELADO),
        EstadoPedido.ACEPTADO, EnumSet.of(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO),
        EstadoPedido.EN_PREPARACION, EnumSet.of(EstadoPedido.DESPACHADO, EstadoPedido.CANCELADO),
        EstadoPedido.DESPACHADO, EnumSet.of(EstadoPedido.ENTREGADO),
        EstadoPedido.ENTREGADO, EnumSet.noneOf(EstadoPedido.class),
        EstadoPedido.CANCELADO, EnumSet.noneOf(EstadoPedido.class)
    );

    private final VentaRepository ventaRepository;
    private final StockProductoRepository stockRepository;
    private final SesionClienteRepository sesionRepository;
    private final RegistroAuditoriaRepository auditoriaRepository;

    public PedidoService(
        VentaRepository ventaRepository,
        StockProductoRepository stockRepository,
        SesionClienteRepository sesionRepository,
        RegistroAuditoriaRepository auditoriaRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.stockRepository = stockRepository;
        this.sesionRepository = sesionRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    public Page<Venta> listarPedidos(String token, int page, int size) {
        Cliente actor = autenticar(token);
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));

        if ("CLIENTE".equals(actor.getRol())) {
            return ventaRepository.findByClienteIdOrderByFechaDescIdDesc(actor.getId(), pagina);
        }
        exigirRol(actor, "ADMIN", "OPERADOR");
        if ("OPERADOR".equals(actor.getRol())) {
            return ventaRepository.findByEstadoNotInOrderByFechaDescIdDesc(
                List.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO),
                pagina
            );
        }
        return ventaRepository.findAllByOrderByFechaDescIdDesc(pagina);
    }

    public List<Venta> listarPedidosRecientesCliente(String token, int limite) {
        Cliente actor = autenticar(token);
        exigirRol(actor, "CLIENTE");
        return ventaRepository.findByClienteIdOrderByFechaDescIdDesc(actor.getId()).stream()
            .limit(Math.min(Math.max(limite, 1), 20))
            .toList();
    }

    @Transactional
    public Venta cambiarEstado(Long ventaId, EstadoPedido nuevoEstado, String token) {
        Cliente actor = autenticar(token);
        exigirRol(actor, "ADMIN", "OPERADOR");

        Venta venta = ventaRepository.findById(ventaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        EstadoPedido estadoActual = venta.getEstado() == null ? EstadoPedido.CREADO : venta.getEstado();

        if (!TRANSICIONES.getOrDefault(estadoActual, Set.of()).contains(nuevoEstado)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Transicion no permitida: " + estadoActual + " -> " + nuevoEstado
            );
        }

        if (nuevoEstado == EstadoPedido.ACEPTADO) {
            descontarStock(venta);
        }

        venta.setEstado(nuevoEstado);
        Venta guardada = ventaRepository.save(venta);
        auditar(actor, guardada.getId(), "CAMBIO_ESTADO", estadoActual + " -> " + nuevoEstado);
        return guardada;
    }

    public Cliente autenticar(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere una sesion activa");
        }

        SesionCliente sesion = sesionRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida"));
        return sesion.getCliente();
    }

    public void auditarCreacion(Venta venta) {
        Cliente actor = venta.getCliente();
        if (actor != null) {
            auditar(actor, venta.getId(), "PEDIDO_CREADO", "Estado inicial CREADO");
        }
    }

    private void descontarStock(Venta venta) {
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return;
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle.getProducto() == null || detalle.getProducto().getId() == null) {
                continue;
            }

            int cantidad = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
            StockProducto stock = stockRepository.findByProductoId(detalle.getProducto().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Producto sin registro de stock"));

            if (cantidad < 1 || stock.getStock() == null || stock.getStock() < cantidad) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock insuficiente para " + detalle.getProducto().getNombre()
                );
            }
            stock.setStock(stock.getStock() - cantidad);
            stockRepository.save(stock);
        }
    }

    private void exigirRol(Cliente actor, String... rolesPermitidos) {
        for (String rol : rolesPermitidos) {
            if (rol.equalsIgnoreCase(actor.getRol())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para esta operacion");
    }

    private void auditar(Cliente actor, Long ventaId, String accion, String detalle) {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setAccion(accion);
        registro.setEntidad("VENTA");
        registro.setEntidadId(ventaId);
        registro.setActorId(actor.getId());
        registro.setActorNombre((actor.getNombres() + " " + actor.getApellidos()).trim());
        registro.setActorRol(actor.getRol());
        registro.setDetalle(detalle);
        auditoriaRepository.save(registro);
    }
}
