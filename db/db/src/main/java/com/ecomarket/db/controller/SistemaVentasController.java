package com.ecomarket.db.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecomarket.db.model.Boleta;
import com.ecomarket.db.model.Carrito;
import com.ecomarket.db.model.CarritoProducto;
import com.ecomarket.db.model.CarritoProductoId;
import com.ecomarket.db.model.CategoriaProducto;
import com.ecomarket.db.model.Cliente;
import com.ecomarket.db.model.Contacto;
import com.ecomarket.db.model.DetalleVenta;
import com.ecomarket.db.model.Direccion;
import com.ecomarket.db.model.Producto;
import com.ecomarket.db.model.SesionCliente;
import com.ecomarket.db.model.StockProducto;
import com.ecomarket.db.model.Venta;
import com.ecomarket.db.repository.BoletaRepository;
import com.ecomarket.db.repository.CarritoRepository;
import com.ecomarket.db.repository.CategoriaRepository;
import com.ecomarket.db.repository.ClienteRepository;
import com.ecomarket.db.repository.ContactoRepository;
import com.ecomarket.db.repository.DireccionRepository;
import com.ecomarket.db.repository.ProductoRepository;
import com.ecomarket.db.repository.SesionClienteRepository;
import com.ecomarket.db.repository.StockProductoRepository;
import com.ecomarket.db.repository.VentaRepository;

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
    @Autowired private ContactoRepository contactoRepo;
    @Autowired private CarritoRepository carritoRepo;
    @Autowired private SesionClienteRepository sesionRepo;

    // --- CLIENTES ---
    @GetMapping("/clientes")
    public List<Cliente> listarClientes() { return clienteRepo.findAll(); }

    @GetMapping("/clientes/count")
    public Map<String, Long> contarClientes() {
        return Map.of("total", clienteRepo.count());
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
    public Producto crearProducto(@RequestBody Producto producto) {
        if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
            CategoriaProducto catReal = categoriaRepo.findById(producto.getCategoria().getId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            
            producto.setCategoria(catReal);
        }
        
        return productoRepo.save(producto);
    }

    // --- STOCK PRODUCTO ---
    @GetMapping({"/stock-producto", "/bodega"})
    public List<StockProducto> verStockGlobal() { return stockProductoRepo.findAll(); }

    @PostMapping({"/stock-producto", "/bodega"})
    public StockProducto crearStockProducto(@RequestBody StockProducto stockProducto) {
        if (stockProducto.getProducto() == null || stockProducto.getProducto().getId() == null) {
            throw new RuntimeException("Debe asignar un producto válido al stock");
        }

        Producto productoReal = productoRepo.findById(stockProducto.getProducto().getId())
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        stockProducto.setProducto(productoReal);

        return stockProductoRepo.save(stockProducto);
    }

    @PatchMapping({"/stock-producto/{id}", "/bodega/{id}"})
    public StockProducto actualizarStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        StockProducto stockProducto = stockProductoRepo.findById(id).orElseThrow();
        stockProducto.setStock(body.get("stock"));
        return stockProductoRepo.save(stockProducto);
    }

    // --- VENTAS ---
    @PostMapping("/ventas")
    @Transactional
    public Venta registrarVenta(@RequestBody Venta venta) {
        if (venta.getCliente() != null && venta.getCliente().getId() != null) {
            Cliente clienteReal = clienteRepo.findById(venta.getCliente().getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            venta.setCliente(clienteReal);
        }

        if (venta.getDireccion() != null && venta.getDireccion().getId() != null) {
            Direccion direccionReal = direccionRepo.findById(venta.getDireccion().getId())
                .orElseThrow(() -> new RuntimeException("Direccion no encontrada"));

            venta.setDireccion(direccionReal);
        }

        // Es vital asignar la venta a cada detalle antes de guardar
        if (venta.getDetalles() != null) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                detalle.setVenta(venta);
                if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
                    Producto productoReal = productoRepo.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                    detalle.setProducto(productoReal);
                }
                if (detalle.getFecha() == null) {
                    detalle.setFecha(LocalDate.now());
                }
            }
        }
        return ventaRepo.save(venta);
    }

    @GetMapping("/clientes/{id}/ventas")
    public List<Venta> historialPorCliente(@PathVariable Long id) {
        return ventaRepo.findByClienteId(id);
    }

    @GetMapping("/ventas-completas")
    public List<Venta> listarVentasCompletas() {
        return ventaRepo.findAll();
    }

    @GetMapping("/ventas/count")
    public Map<String, Long> contarVentas() {
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
    public CategoriaProducto crearCategoria(@RequestBody CategoriaProducto categoria) {
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

    private void asignarRolCliente(Cliente cliente) {
        String nombres = cliente.getNombres();

        if (nombres != null) {
            String primerNombre = nombres.trim().split("\\s+")[0];

            if ("admin".equalsIgnoreCase(primerNombre)) {
                cliente.setRol("ADMIN");
                return;
            }
        }

        cliente.setRol("CLIENTE");
    }
}
