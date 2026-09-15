package com.pedidos360.db.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.pedidos360.db.model.Cliente;
import com.pedidos360.db.model.DetalleVenta;
import com.pedidos360.db.model.EstadoPedido;
import com.pedidos360.db.model.Producto;
import com.pedidos360.db.model.SesionCliente;
import com.pedidos360.db.model.StockProducto;
import com.pedidos360.db.model.Venta;
import com.pedidos360.db.repository.RegistroAuditoriaRepository;
import com.pedidos360.db.repository.SesionClienteRepository;
import com.pedidos360.db.repository.StockProductoRepository;
import com.pedidos360.db.repository.VentaRepository;

class PedidoServiceTest {
    private VentaRepository ventaRepository;
    private StockProductoRepository stockRepository;
    private SesionClienteRepository sesionRepository;
    private RegistroAuditoriaRepository auditoriaRepository;
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        ventaRepository = org.mockito.Mockito.mock(VentaRepository.class);
        stockRepository = org.mockito.Mockito.mock(StockProductoRepository.class);
        sesionRepository = org.mockito.Mockito.mock(SesionClienteRepository.class);
        auditoriaRepository = org.mockito.Mockito.mock(RegistroAuditoriaRepository.class);
        pedidoService = new PedidoService(ventaRepository, stockRepository, sesionRepository, auditoriaRepository);
    }

    @Test
    void aceptarPedidoDescuentaStockYRegistraAuditoria() {
        Cliente operador = cliente("OPERADOR");
        Venta venta = ventaCreada();
        StockProducto stock = new StockProducto();
        stock.setStock(5);
        stock.setProducto(venta.getDetalles().get(0).getProducto());

        when(sesionRepository.findByToken("token-operador")).thenReturn(Optional.of(sesion(operador)));
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(stockRepository.findByProductoId(10L)).thenReturn(Optional.of(stock));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Venta resultado = pedidoService.cambiarEstado(1L, EstadoPedido.ACEPTADO, "token-operador");

        assertEquals(EstadoPedido.ACEPTADO, resultado.getEstado());
        assertEquals(3, stock.getStock());
        verify(auditoriaRepository).save(any());
    }

    @Test
    void noPermiteDespacharPedidoCreado() {
        when(sesionRepository.findByToken("token-operador"))
            .thenReturn(Optional.of(sesion(cliente("OPERADOR"))));
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(ventaCreada()));

        assertThrows(
            ResponseStatusException.class,
            () -> pedidoService.cambiarEstado(1L, EstadoPedido.DESPACHADO, "token-operador")
        );
    }

    @Test
    void clienteNoPuedeCambiarEstado() {
        when(sesionRepository.findByToken("token-cliente"))
            .thenReturn(Optional.of(sesion(cliente("CLIENTE"))));

        assertThrows(
            ResponseStatusException.class,
            () -> pedidoService.cambiarEstado(1L, EstadoPedido.ACEPTADO, "token-cliente")
        );
    }

    private Cliente cliente(String rol) {
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setNombres("Usuario");
        cliente.setApellidos("Prueba");
        cliente.setRol(rol);
        return cliente;
    }

    private SesionCliente sesion(Cliente cliente) {
        SesionCliente sesion = new SesionCliente();
        sesion.setToken("token");
        sesion.setCliente(cliente);
        return sesion;
    }

    private Venta ventaCreada() {
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Producto de prueba");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setCantidad(2);
        detalle.setProducto(producto);

        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstado(EstadoPedido.CREADO);
        venta.setCliente(cliente("CLIENTE"));
        venta.setDetalles(List.of(detalle));
        return venta;
    }
}
