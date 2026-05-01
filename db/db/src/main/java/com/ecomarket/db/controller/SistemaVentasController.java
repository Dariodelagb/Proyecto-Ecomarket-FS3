package com.ecomarket.db.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecomarket.db.model.Bodega;
import com.ecomarket.db.model.Boleta;
import com.ecomarket.db.model.CategoriaProducto;
import com.ecomarket.db.model.Cliente;
import com.ecomarket.db.model.DetalleVenta;
import com.ecomarket.db.model.Producto;
import com.ecomarket.db.model.Venta;
import com.ecomarket.db.repository.BodegaRepository;
import com.ecomarket.db.repository.BoletaRepository;
import com.ecomarket.db.repository.CategoriaRepository;
import com.ecomarket.db.repository.ClienteRepository;
import com.ecomarket.db.repository.ProductoRepository;
import com.ecomarket.db.repository.VentaRepository;

@RestController
@RequestMapping("/api")
public class SistemaVentasController {

    @Autowired private ClienteRepository clienteRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private BodegaRepository bodegaRepo;
    @Autowired private VentaRepository ventaRepo;
    @Autowired private BoletaRepository boletaRepo;
    @Autowired private CategoriaRepository categoriaRepo;

    // --- CLIENTES ---
    @GetMapping("/clientes")
    public List<Cliente> listarClientes() { return clienteRepo.findAll(); }

    @PostMapping("/clientes")
    public Cliente crearCliente(@RequestBody Cliente cliente) { return clienteRepo.save(cliente); }

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

    // --- BODEGA ---
    @GetMapping("/bodega")
    public List<Bodega> verStockGlobal() { return bodegaRepo.findAll(); }

    @PostMapping("/bodega")
    public Bodega crearBodega(@RequestBody Bodega bodega) {
        // Revisar si el producto existe antes de crear la bodega
        if (bodega.getProducto() == null || bodega.getProducto().getId() == null) {
            throw new RuntimeException("Debe asignar un producto válido a la bodega");
        }
        
        return bodegaRepo.save(bodega);
    }

    @PatchMapping("/bodega/{id}")
    public Bodega actualizarStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Bodega b = bodegaRepo.findById(id).orElseThrow();
        b.setStock(body.get("stock"));
        return bodegaRepo.save(b);
    }

    // --- VENTAS ---
    @PostMapping("/ventas")
    @Transactional
    public Venta registrarVenta(@RequestBody Venta venta) {
        // Es vital asignar la venta a cada detalle antes de guardar
        if (venta.getDetalles() != null) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                detalle.setVenta(venta);
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
}