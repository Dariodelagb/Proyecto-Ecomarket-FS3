package com.pedidos360.db.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pedidos360.db.model.Boleta;
import com.pedidos360.db.model.Carrito;
import com.pedidos360.db.model.CarritoProducto;
import com.pedidos360.db.model.CarritoProductoId;
import com.pedidos360.db.model.CategoriaProducto;
import com.pedidos360.db.model.Cliente;
import com.pedidos360.db.model.Contacto;
import com.pedidos360.db.model.DetalleVenta;
import com.pedidos360.db.model.Direccion;
import com.pedidos360.db.model.EstadoPedido;
import com.pedidos360.db.model.Producto;
import com.pedidos360.db.model.RegistroAuditoria;
import com.pedidos360.db.model.SesionCliente;
import com.pedidos360.db.model.StockProducto;
import com.pedidos360.db.model.Venta;
import com.pedidos360.db.repository.BoletaRepository;
import com.pedidos360.db.repository.CarritoRepository;
import com.pedidos360.db.repository.CategoriaRepository;
import com.pedidos360.db.repository.ClienteRepository;
import com.pedidos360.db.repository.ContactoRepository;
import com.pedidos360.db.repository.DireccionRepository;
import com.pedidos360.db.repository.DetalleVentaRepository;
import com.pedidos360.db.repository.ProductoRepository;
import com.pedidos360.db.repository.RegistroAuditoriaRepository;
import com.pedidos360.db.repository.SesionClienteRepository;
import com.pedidos360.db.repository.StockProductoRepository;
import com.pedidos360.db.repository.VentaRepository;
import com.pedidos360.db.service.PedidoService;

@RestController
@RequestMapping("/api")
public class SistemaVentasController {

    @Autowired private ClienteRepository clienteRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private StockProductoRepository stockProductoRepo;
    @Autowired private VentaRepository ventaRepo;
    @Autowired private BoletaRepository boletaRepo;
    @Autowired private CategoriaRepository categoriaRepo;
    @Autowired private DireccionRepository direccionRepo;
    @Autowired private DetalleVentaRepository detalleVentaRepo;
    @Autowired private ContactoRepository contactoRepo;
    @Autowired private CarritoRepository carritoRepo;
    @Autowired private SesionClienteRepository sesionRepo;
    @Autowired private RegistroAuditoriaRepository auditoriaRepo;
    @Autowired private PedidoService pedidoService;

    @Value("${pedidos360.reportes.internal-key:pedidos360-internal}")
    private String reportesInternalKey;

    // --- CLIENTES ---
    @GetMapping("/clientes")
    public List<Cliente> listarClientes(
        @RequestHeader(value = "X-Session-Token", required = false) String token,
        @RequestHeader(value = "X-Report-Key", required = false) String reportKey
    ) {
        exigirAdminOReporte(token, reportKey);
        return clienteRepo.findAll();
    }

    @GetMapping("/clientes/count")
    public Map<String, Long> contarClientes(
        @RequestHeader(value = "X-Session-Token", required = false) String token,
        @RequestHeader(value = "X-Report-Key", required = false) String reportKey
    ) {
        exigirAdminOReporte(token, reportKey);
        return Map.of("total", clienteRepo.count());
    }

    @GetMapping("/clientes/activos")
    public List<Cliente> listarClientesActivos(@RequestHeader("X-Session-Token") String token) {
        exigirAdmin(token);
        return sesionRepo.findClientesActivos();
    }

    @GetMapping("/clientes/activos/count")
    public Map<String, Integer> contarClientesActivos(@RequestHeader("X-Session-Token") String token) {
        exigirAdmin(token);
        return Map.of("total", sesionRepo.findClientesActivos().size());
    }

    @PostMapping("/clientes")
    public Cliente crearCliente(@RequestBody Cliente cliente) {
        asignarRolCliente(cliente);

        if (cliente.getDirecciones() != null) {
            for (Direccion direccion : cliente.getDirecciones()) {
                direccion.setCliente(cliente);
            }
        }

        return clienteRepo.save(cliente);
    }

    @PatchMapping("/clientes/{id}/rol")
    public Cliente actualizarRolCliente(
        @PathVariable Long id,
        @RequestHeader("X-Session-Token") String token,
        @RequestBody Map<String, String> body
    ) {
        exigirAdmin(token);
        String nuevoRol = body.getOrDefault("rol", "").trim().toUpperCase();
        if (!List.of("ADMIN", "OPERADOR", "CLIENTE").contains(nuevoRol)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Rol invalido"
            );
        }
        Cliente cliente = clienteRepo.findById(id).orElseThrow();
        cliente.setRol(nuevoRol);
        return clienteRepo.save(cliente);
    }

    // --- AUTENTICACION ---
    @PostMapping("/auth/registro")
    @Transactional
    public Map<String, Object> registrarCliente(@RequestBody Cliente cliente) {
        if (cliente.getEmail() == null || cliente.getContrasena() == null) {
            throw new RuntimeException("Email y contrasena son obligatorios");
        }

        clienteRepo.findByEmail(cliente.getEmail()).ifPresent(existente -> {
            throw new RuntimeException("Ya existe un cliente con ese email");
        });

        asignarRolCliente(cliente);

        if (cliente.getDirecciones() != null) {
            for (Direccion direccion : cliente.getDirecciones()) {
                direccion.setCliente(cliente);
            }
        }

        Cliente clienteGuardado = clienteRepo.save(cliente);
        SesionCliente sesion = crearSesion(clienteGuardado);

        return Map.of(
            "token", sesion.getToken(),
            "cliente", clienteGuardado
        );
    }

    @PostMapping("/auth/login")
    public Map<String, Object> iniciarSesion(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Integer rut = Integer.valueOf(body.get("rut"));
        String dvrut = body.get("dvrut");
        String contrasena = body.get("contrasena");

        Cliente cliente = clienteRepo
            .findByEmailAndRutAndDvrutAndContrasena(email, rut, dvrut, contrasena)
            .orElseThrow(() -> new RuntimeException("Credenciales invalidas"));

        SesionCliente sesion = crearSesion(cliente);

        return Map.of(
            "token", sesion.getToken(),
            "cliente", cliente
        );
    }

    @GetMapping("/auth/sesion/{token}")
    public Map<String, Object> verSesion(@PathVariable String token) {
        SesionCliente sesion = sesionRepo.findByToken(token).orElseThrow();
        return Map.of(
            "token", sesion.getToken(),
            "cliente", sesion.getCliente()
        );
    }

    @DeleteMapping("/auth/sesion/{token}")
    @Transactional
    public Map<String, Boolean> cerrarSesion(@PathVariable String token) {
        sesionRepo.deleteByToken(token);
        return Map.of("ok", true);
    }

    // --- DIRECCIONES ---
    @GetMapping("/direcciones")
    public List<Direccion> listarDirecciones() { return direccionRepo.findAll(); }

    @GetMapping("/clientes/{id}/direcciones")
    public List<Direccion> direccionesPorCliente(@PathVariable Long id) {
        return direccionRepo.findByClienteId(id);
    }

    @PostMapping("/clientes/{id}/direcciones")
    public Direccion crearDireccion(@PathVariable Long id, @RequestBody Direccion direccion) {
        Cliente cliente = clienteRepo.findById(id).orElseThrow();
        direccion.setCliente(cliente);
        return direccionRepo.save(direccion);
    }

    // --- CONTACTOS ---
    @GetMapping("/contactos")
    public List<Contacto> listarContactos() { return contactoRepo.findAll(); }

    @PostMapping("/contactos")
    public Contacto crearContacto(@RequestBody Contacto contacto) {
        return contactoRepo.save(contacto);
    }

    // --- CARRITOS ---
    @GetMapping("/carritos")
    public List<Carrito> listarCarritos() { return carritoRepo.findAll(); }

    @GetMapping("/carritos/cliente/{clienteId}")
    public Carrito carritoPorCliente(@PathVariable Long clienteId) {
        return obtenerOCrearCarrito(clienteId);
    }

    @PostMapping("/carritos/cliente/{clienteId}/productos/{productoId}")
    @Transactional
    public Carrito agregarProductoAlCarrito(
        @PathVariable Long clienteId,
        @PathVariable Long productoId,
        @RequestBody(required = false) Map<String, Integer> body
    ) {
        Carrito carrito = obtenerOCrearCarrito(clienteId);
        Producto producto = productoRepo.findById(productoId).orElseThrow();
        int cantidad = body != null && body.get("cantidad") != null ? body.get("cantidad") : 1;

        if (cantidad < 1) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
        }

        CarritoProducto item = carrito.getItems().stream()
            .filter(actual -> actual.getProducto() != null && actual.getProducto().getId().equals(productoId))
            .findFirst()
            .orElse(null);

        if (item == null) {
            item = new CarritoProducto();
            CarritoProductoId itemId = new CarritoProductoId();
            itemId.setCarritoId(carrito.getId());
            itemId.setProductoId(productoId);
            item.setId(itemId);
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            carrito.getItems().add(item);
        } else {
            int cantidadActual = item.getCantidad() == null ? 0 : item.getCantidad();
            item.setCantidad(cantidadActual + cantidad);
        }

        return carritoRepo.save(carrito);
    }

    @DeleteMapping("/carritos/cliente/{clienteId}/productos/{productoId}")
    @Transactional
    public Carrito quitarProductoDelCarrito(@PathVariable Long clienteId, @PathVariable Long productoId) {
        Carrito carrito = obtenerOCrearCarrito(clienteId);
        carrito.getItems().removeIf(item -> item.getProducto() != null && item.getProducto().getId().equals(productoId));
        return carritoRepo.save(carrito);
    }

    @DeleteMapping("/carritos/cliente/{clienteId}")
    @Transactional
    public Map<String, Boolean> vaciarCarrito(@PathVariable Long clienteId) {
        Carrito carrito = obtenerOCrearCarrito(clienteId);
        carrito.getItems().clear();
        carritoRepo.save(carrito);
        return Map.of("ok", true);
    }

    // --- PRODUCTOS ---
    @GetMapping("/productos")
    public List<Producto> listarProductos() { return productoRepo.findAll(); }

    @PostMapping("/productos")
    public Producto crearProducto(
        @RequestHeader("X-Session-Token") String token,
        @RequestBody Producto producto
    ) {
        exigirAdmin(token);
        if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
            CategoriaProducto catReal = categoriaRepo.findById(producto.getCategoria().getId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            
            producto.setCategoria(catReal);
        }
        
        return productoRepo.save(producto);
    }

    @PatchMapping("/productos/{id}")
    public Producto actualizarProducto(
        @PathVariable Long id,
        @RequestHeader("X-Session-Token") String token,
        @RequestBody Producto cambios
    ) {
        exigirAdmin(token);
        Producto producto = productoRepo.findById(id).orElseThrow();
        if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) {
            producto.setNombre(cambios.getNombre().trim());
        }
        if (cambios.getPrecio() != null) {
            producto.setPrecio(cambios.getPrecio());
        }
        if (cambios.getCategoria() != null && cambios.getCategoria().getId() != null) {
            producto.setCategoria(categoriaRepo.findById(cambios.getCategoria().getId()).orElseThrow());
        }
        return productoRepo.save(producto);
    }

    @DeleteMapping("/productos/{id}")
    @Transactional
    public Map<String, Boolean> eliminarProducto(
        @PathVariable Long id,
        @RequestHeader("X-Session-Token") String token
    ) {
        exigirAdmin(token);
        if (detalleVentaRepo.existsByProductoId(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "No se puede eliminar un producto que pertenece al historial de ventas"
            );
        }
        stockProductoRepo.findByProductoId(id).ifPresent(stockProductoRepo::delete);
        productoRepo.deleteById(id);
        return Map.of("ok", true);
    }

    // --- STOCK PRODUCTO ---
    @GetMapping({"/stock-producto", "/bodega"})
    public List<StockProducto> verStockGlobal() { return stockProductoRepo.findAll(); }

    @PostMapping({"/stock-producto", "/bodega"})
    public StockProducto crearStockProducto(
        @RequestHeader("X-Session-Token") String token,
        @RequestBody StockProducto stockProducto
    ) {
        exigirAdmin(token);
        if (stockProducto.getProducto() == null || stockProducto.getProducto().getId() == null) {
            throw new RuntimeException("Debe asignar un producto válido al stock");
        }

        Producto productoReal = productoRepo.findById(stockProducto.getProducto().getId())
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        stockProducto.setProducto(productoReal);

        return stockProductoRepo.save(stockProducto);
    }

    @PatchMapping({"/stock-producto/{id}", "/bodega/{id}"})
    public StockProducto actualizarStock(
        @PathVariable Long id,
        @RequestHeader("X-Session-Token") String token,
        @RequestBody Map<String, Integer> body
    ) {
        exigirAdmin(token);
        StockProducto stockProducto = stockProductoRepo.findById(id).orElseThrow();
        stockProducto.setStock(body.get("stock"));
        return stockProductoRepo.save(stockProducto);
    }

    // --- VENTAS ---
    @PostMapping("/ventas")
    @Transactional
    public Venta registrarVenta(
        @RequestBody Venta venta,
        @RequestHeader("X-Session-Token") String token
    ) {
        Cliente actor = pedidoService.autenticar(token);
        boolean esAdmin = "ADMIN".equals(actor.getRol());
        boolean esCliente = "CLIENTE".equals(actor.getRol());
        if (!esAdmin && !esCliente) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Solo clientes y administradores pueden crear ventas"
            );
        }

        venta.setEstado(EstadoPedido.CREADO);
        if (venta.getFecha() == null) {
            venta.setFecha(LocalDate.now());
        }

        if (esCliente) {
            venta.setCliente(actor);
        } else if (venta.getCliente() != null && venta.getCliente().getId() != null) {
            Cliente clienteReal = clienteRepo.findById(venta.getCliente().getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            venta.setCliente(clienteReal);
        } else {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Debes seleccionar un cliente"
            );
        }

        if (venta.getDireccion() != null && venta.getDireccion().getId() != null) {
            Direccion direccionReal = direccionRepo.findById(venta.getDireccion().getId())
                .orElseThrow(() -> new RuntimeException("Direccion no encontrada"));

            if (direccionReal.getCliente() == null
                || !direccionReal.getCliente().getId().equals(venta.getCliente().getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "La direccion no pertenece al cliente seleccionado"
                );
            }

            venta.setDireccion(direccionReal);
        }

        // Es vital asignar la venta a cada detalle antes de guardar
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "La venta debe contener al menos un producto"
            );
        } else {
            double montoCalculado = 0;
            for (DetalleVenta detalle : venta.getDetalles()) {
                detalle.setVenta(venta);
                if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
                    Producto productoReal = productoRepo.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                    detalle.setProducto(productoReal);
                    int cantidad = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
                    if (cantidad < 1) {
                        throw new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST,
                            "La cantidad de cada producto debe ser mayor que cero"
                        );
                    }
                    detalle.setPrecioUnitario(productoReal.getPrecio().doubleValue());
                    montoCalculado += productoReal.getPrecio() * cantidad;
                } else {
                    throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Cada detalle debe indicar un producto"
                    );
                }
                if (detalle.getFecha() == null) {
                    detalle.setFecha(LocalDate.now());
                }
            }
            venta.setMonto(montoCalculado);
        }
        Venta guardada = ventaRepo.save(venta);
        pedidoService.auditarCreacion(guardada, actor);
        return guardada;
    }

    @GetMapping("/clientes/{id}/ventas")
    public List<Venta> historialPorCliente(
        @PathVariable Long id,
        @RequestHeader("X-Session-Token") String token
    ) {
        Cliente actor = pedidoService.autenticar(token);
        boolean mismoCliente = "CLIENTE".equals(actor.getRol()) && actor.getId().equals(id);
        boolean personalAutorizado = "ADMIN".equals(actor.getRol()) || "OPERADOR".equals(actor.getRol());
        if (!mismoCliente && !personalAutorizado) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "No puedes consultar pedidos de otro cliente"
            );
        }
        return ventaRepo.findByClienteId(id);
    }

    @GetMapping("/ventas-completas")
    public List<Venta> listarVentasCompletas(
        @RequestHeader(value = "X-Session-Token", required = false) String token,
        @RequestHeader(value = "X-Report-Key", required = false) String reportKey
    ) {
        exigirAdminOReporte(token, reportKey);
        return ventaRepo.findAll();
    }

    @GetMapping("/pedidos")
    public Page<Venta> listarPedidos(
        @RequestHeader("X-Session-Token") String token,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "15") int size
    ) {
        return pedidoService.listarPedidos(token, page, size);
    }

    @GetMapping("/pedidos/recientes")
    public List<Venta> listarPedidosRecientes(
        @RequestHeader("X-Session-Token") String token,
        @RequestParam(defaultValue = "5") int limit
    ) {
        return pedidoService.listarPedidosRecientesCliente(token, limit);
    }

    @PatchMapping("/pedidos/{id}/estado")
    public Venta cambiarEstadoPedido(
        @PathVariable Long id,
        @RequestHeader("X-Session-Token") String token,
        @RequestBody Map<String, String> body
    ) {
        String valorEstado = body.getOrDefault("estado", "").trim().toUpperCase().replace(' ', '_');
        try {
            return pedidoService.cambiarEstado(id, EstadoPedido.valueOf(valorEstado), token);
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Estado de pedido invalido"
            );
        }
    }

    @GetMapping("/auditoria")
    public List<RegistroAuditoria> listarAuditoria(
        @RequestHeader("X-Session-Token") String token
    ) {
        Cliente actor = pedidoService.autenticar(token);
        if (!"ADMIN".equalsIgnoreCase(actor.getRol())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Solo un administrador puede consultar la auditoria"
            );
        }
        return auditoriaRepo.findAll();
    }

    @GetMapping("/auditoria/reporte")
    public List<RegistroAuditoria> exportarAuditoria(
        @RequestHeader("X-Report-Key") String reportKey
    ) {
        if (!reportesInternalKey.equals(reportKey)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Credencial interna invalida"
            );
        }
        return auditoriaRepo.findAll();
    }

    @GetMapping("/ventas/count")
    public Map<String, Long> contarVentas(
        @RequestHeader(value = "X-Session-Token", required = false) String token,
        @RequestHeader(value = "X-Report-Key", required = false) String reportKey
    ) {
        exigirAdminOReporte(token, reportKey);
        return Map.of("total", ventaRepo.count());
    }

    // --- BOLETAS ---
    @PostMapping("/boletas")
    public Boleta generarBoleta(@RequestBody Boleta boleta) {
        boleta.setFecha(LocalDate.now());
        return boletaRepo.save(boleta);
    }

    // --- CATEGORIAS ---

    // GET /categorias: Listar las categorías disponibles
    @GetMapping("/categorias")
    public List<CategoriaProducto> listarCategorias() {
        return categoriaRepo.findAll();
    }

    // POST /categorias: Crear una nueva categoría (útil para tener datos iniciales)
    @PostMapping("/categorias")
    public CategoriaProducto crearCategoria(
        @RequestHeader("X-Session-Token") String token,
        @RequestBody CategoriaProducto categoria
    ) {
        exigirAdmin(token);
        return categoriaRepo.save(categoria);
    }

    private Carrito obtenerOCrearCarrito(Long clienteId) {
        return carritoRepo.findByClienteId(clienteId)
            .orElseGet(() -> {
                Cliente cliente = clienteRepo.findById(clienteId).orElseThrow();
                Carrito carrito = new Carrito();
                carrito.setCliente(cliente);
                return carritoRepo.save(carrito);
            });
    }

    private SesionCliente crearSesion(Cliente cliente) {
        SesionCliente sesion = new SesionCliente();
        sesion.setCliente(cliente);
        sesion.setToken(UUID.randomUUID().toString());
        return sesionRepo.save(sesion);
    }

    private void exigirAdmin(String token) {
        Cliente actor = pedidoService.autenticar(token);
        if (!"ADMIN".equalsIgnoreCase(actor.getRol())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Solo un administrador puede consultar esta informacion"
            );
        }
    }

    private void exigirAdminOReporte(String token, String reportKey) {
        if (reportKey != null && reportesInternalKey.equals(reportKey)) {
            return;
        }
        exigirAdmin(token);
    }

    private void asignarRolCliente(Cliente cliente) {
        cliente.setRol("CLIENTE");
    }
}
