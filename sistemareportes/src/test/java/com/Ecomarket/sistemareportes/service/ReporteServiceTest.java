package com.Ecomarket.sistemareportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.Ecomarket.sistemareportes.client.ClientesClient;
import com.Ecomarket.sistemareportes.client.InventarioClient;
import com.Ecomarket.sistemareportes.client.VentasClient;
import com.Ecomarket.sistemareportes.dto.BodegaDTO;
import com.Ecomarket.sistemareportes.dto.CategoriaDTO;
import com.Ecomarket.sistemareportes.dto.ClienteDTO;
import com.Ecomarket.sistemareportes.dto.DetalleVentaDTO;
import com.Ecomarket.sistemareportes.dto.ProductoDTO;
import com.Ecomarket.sistemareportes.dto.ResumenDTO;
import com.Ecomarket.sistemareportes.dto.VentaDTO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private VentasClient ventasClient;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private ClientesClient clientesClient;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    void obtenerResumenCalculaTotalesDeVentasYProductos() {
        when(ventasClient.obtenerTodasLasVentas()).thenReturn(List.of(
            venta(1L, 12000.0, LocalDate.of(2026, 1, 10)),
            venta(2L, null, LocalDate.of(2026, 1, 12)),
            venta(3L, 8000.0, LocalDate.of(2026, 2, 5))
        ));
        when(inventarioClient.obtenerTodosLosProductos()).thenReturn(List.of(
            producto(1L, "Bloqueador Solar Mineral SPF 30", 11900, "Bloqueadores"),
            producto(2L, "Aceite Herbal de Lavanda", 7900, "Aceites Herbales")
        ));

        ResumenDTO resumen = reporteService.obtenerResumen();

        assertEquals(20000.0, resumen.getTotalDinero());
        assertEquals(3, resumen.getTotalVentas());
        assertEquals(2, resumen.getTotalProductos());
    }

    @Test
    void generarReporteExcelCreaHojasPrincipales() throws IOException {
        prepararDatosExcel();

        byte[] excel = reporteService.generarReporteExcel();

        assertNotNull(excel);
        assertTrue(excel.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertNotNull(workbook.getSheet("Resumen"));
            assertNotNull(workbook.getSheet("Ventas"));
            assertNotNull(workbook.getSheet("Usuarios"));
            assertNotNull(workbook.getSheet("Productos y stock"));
            assertNotNull(workbook.getSheet("Ventas mensuales"));
        }
    }

    @Test
    void generarReporteExcelNoExponeDatosSensiblesEnHojaUsuarios() throws IOException {
        prepararDatosExcel();

        byte[] excel = reporteService.generarReporteExcel();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Row header = workbook.getSheet("Usuarios").getRow(0);

            assertEquals("ID", header.getCell(0).getStringCellValue());
            assertEquals("Nombre", header.getCell(1).getStringCellValue());
            assertEquals("Rol", header.getCell(2).getStringCellValue());
            assertEquals(3, header.getPhysicalNumberOfCells());
        }
    }

    @Test
    void generarReporteExcelIncluyeIdsEnHojaVentas() throws IOException {
        prepararDatosExcel();

        byte[] excel = reporteService.generarReporteExcel();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Row header = workbook.getSheet("Ventas").getRow(0);
            Row primeraVenta = workbook.getSheet("Ventas").getRow(1);

            assertEquals("ID", header.getCell(0).getStringCellValue());
            assertEquals("ID Cliente", header.getCell(2).getStringCellValue());
            assertEquals(1L, (long) primeraVenta.getCell(0).getNumericCellValue());
            assertEquals(1L, (long) primeraVenta.getCell(2).getNumericCellValue());
        }
    }

    private void prepararDatosExcel() {
        ClienteDTO cliente = cliente(1L, "Maria", "Gonzalez", "CLIENTE");
        ProductoDTO producto = producto(1L, "Bloqueador Solar Mineral SPF 30", 11900, "Bloqueadores");
        VentaDTO venta = venta(1L, 23800.0, LocalDate.of(2026, 6, 18));
        venta.setCliente(cliente);
        venta.setDetalles(List.of(detalle(1L, 2, 11900.0, LocalDate.of(2026, 6, 18), producto)));

        when(ventasClient.obtenerTodasLasVentas()).thenReturn(List.of(venta));
        when(clientesClient.obtenerTodosLosClientes()).thenReturn(List.of(cliente));
        when(inventarioClient.obtenerTodosLosProductos()).thenReturn(List.of(producto));
        when(inventarioClient.obtenerTodoElStock()).thenReturn(List.of(stock(1L, 50, producto)));
    }

    private VentaDTO venta(Long id, Double monto, LocalDate fecha) {
        VentaDTO venta = new VentaDTO();
        venta.setId(id);
        venta.setMonto(monto);
        venta.setFecha(fecha);
        venta.setTipoEnvio("Envio a domicilio");
        return venta;
    }

    private ClienteDTO cliente(Long id, String nombres, String apellidos, String rol) {
        ClienteDTO cliente = new ClienteDTO();
        cliente.setId(id);
        cliente.setNombres(nombres);
        cliente.setApellidos(apellidos);
        cliente.setRol(rol);
        return cliente;
    }

    private ProductoDTO producto(Long id, String nombre, Integer precio, String categoriaNombre) {
        CategoriaDTO categoria = new CategoriaDTO();
        categoria.setId(1L);
        categoria.setCategoria(categoriaNombre);

        ProductoDTO producto = new ProductoDTO();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setCategoria(categoria);
        return producto;
    }

    private DetalleVentaDTO detalle(Long id, Integer cantidad, Double precioUnitario, LocalDate fecha, ProductoDTO producto) {
        DetalleVentaDTO detalle = new DetalleVentaDTO();
        detalle.setId(id);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setFecha(fecha);
        detalle.setProducto(producto);
        return detalle;
    }

    private BodegaDTO stock(Long id, Integer cantidad, ProductoDTO producto) {
        BodegaDTO stock = new BodegaDTO();
        stock.setId(id);
        stock.setStock(cantidad);
        stock.setProducto(producto);
        return stock;
    }
}
