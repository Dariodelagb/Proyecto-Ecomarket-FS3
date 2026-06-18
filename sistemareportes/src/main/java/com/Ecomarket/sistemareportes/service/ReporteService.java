package com.Ecomarket.sistemareportes.service;

import com.Ecomarket.sistemareportes.client.ClientesClient;
import com.Ecomarket.sistemareportes.client.InventarioClient;
import com.Ecomarket.sistemareportes.client.VentasClient;
import com.Ecomarket.sistemareportes.dto.BodegaDTO;
import com.Ecomarket.sistemareportes.dto.ClienteDTO;
import com.Ecomarket.sistemareportes.dto.DetalleVentaDTO;
import com.Ecomarket.sistemareportes.dto.ProductoDTO;
import com.Ecomarket.sistemareportes.dto.ResumenDTO;
import com.Ecomarket.sistemareportes.dto.VentaDTO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {

    @Autowired
    private VentasClient ventasClient;

    @Autowired
    private InventarioClient inventarioClient;

    @Autowired
    private ClientesClient clientesClient;

    public void procesarReporte() {
        List<VentaDTO> listaVentas = ventasClient.obtenerTodasLasVentas();
        List<ProductoDTO> listaProductos = inventarioClient.obtenerTodosLosProductos();
        List<BodegaDTO> listaBodega = inventarioClient.obtenerTodoElStock();
        List<ClienteDTO> listaClientes = clientesClient.obtenerTodosLosClientes();

        System.out.println("=== REPORTE DASHBOARD ===");
        System.out.println("Usuarios registrados: " + listaClientes.size());
        System.out.println("Ventas registradas: " + listaVentas.size());
        System.out.println("Productos registrados: " + listaProductos.size());
        System.out.println("Total vendido: $" + calcularTotalDinero(listaVentas));
        System.out.println("Registros de stock: " + listaBodega.size());
    }

    public ResumenDTO obtenerResumen() {
        List<VentaDTO> listaVentas = ventasClient.obtenerTodasLasVentas();
        List<ProductoDTO> listaProductos = inventarioClient.obtenerTodosLosProductos();

        ResumenDTO resumen = new ResumenDTO();
        resumen.setTotalDinero(calcularTotalDinero(listaVentas));
        resumen.setTotalVentas(listaVentas.size());
        resumen.setTotalProductos(listaProductos.size());

        return resumen;
    }

    public byte[] generarReporteExcel() {
        List<VentaDTO> ventas = ventasClient.obtenerTodasLasVentas();
        List<ClienteDTO> clientes = clientesClient.obtenerTodosLosClientes();
        List<ProductoDTO> productos = inventarioClient.obtenerTodosLosProductos();
        List<BodegaDTO> stock = inventarioClient.obtenerTodoElStock();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ReportStyles styles = crearEstilos(workbook);

            crearHojaResumen(workbook, styles, ventas, clientes, productos, stock);
            crearHojaVentas(workbook, styles, ventas);
            crearHojaUsuarios(workbook, styles, clientes);
            crearHojaProductos(workbook, styles, productos, stock);
            crearHojaVentasMensuales(workbook, styles, ventas);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el archivo Excel", ex);
        }
    }

    private void crearHojaResumen(
        Workbook workbook,
        ReportStyles styles,
        List<VentaDTO> ventas,
        List<ClienteDTO> clientes,
        List<ProductoDTO> productos,
        List<BodegaDTO> stock
    ) {
        Sheet sheet = workbook.createSheet("Resumen");
        crearHeader(sheet, styles.header, "Metrica", "Valor");

        int rowIndex = 1;
        rowIndex = agregarResumen(sheet, styles, rowIndex, "Usuarios registrados", clientes.size(), false);
        rowIndex = agregarResumen(sheet, styles, rowIndex, "Ventas registradas", ventas.size(), false);
        rowIndex = agregarResumen(sheet, styles, rowIndex, "Productos registrados", productos.size(), false);
        rowIndex = agregarResumen(sheet, styles, rowIndex, "Stock total", calcularStockTotal(stock), false);
        rowIndex = agregarResumen(sheet, styles, rowIndex, "Total vendido", calcularTotalDinero(ventas), true);
        agregarResumen(sheet, styles, rowIndex, "Fecha de generacion", LocalDateTime.now().toString(), false);

        autoSize(sheet, 2);
    }

    private void crearHojaVentas(Workbook workbook, ReportStyles styles, List<VentaDTO> ventas) {
        Sheet sheet = workbook.createSheet("Ventas");
        crearHeader(
            sheet,
            styles.header,
            "ID",
            "Fecha",
            "ID Cliente",
            "Cliente",
            "Tipo envio",
            "Productos",
            "Monto"
        );

        int rowIndex = 1;
        for (VentaDTO venta : ventas) {
            Row row = sheet.createRow(rowIndex++);
            crearCell(row, 0, venta.getId(), styles.normal);
            crearCell(row, 1, obtenerFechaVenta(venta), styles.date);
            crearCell(row, 2, idCliente(venta.getCliente()), styles.normal);
            crearCell(row, 3, nombreCliente(venta.getCliente()), styles.normal);
            crearCell(row, 4, texto(venta.getTipoEnvio()), styles.normal);
            crearCell(row, 5, productosVenta(venta), styles.normal);
            crearCell(row, 6, numero(venta.getMonto()), styles.money);
        }

        autoSize(sheet, 7);
    }

    private void crearHojaUsuarios(Workbook workbook, ReportStyles styles, List<ClienteDTO> clientes) {
        Sheet sheet = workbook.createSheet("Usuarios");
        crearHeader(sheet, styles.header, "ID", "Nombre", "Rol");

        int rowIndex = 1;
        for (ClienteDTO cliente : clientes) {
            Row row = sheet.createRow(rowIndex++);
            crearCell(row, 0, cliente.getId(), styles.normal);
            crearCell(row, 1, nombreCliente(cliente), styles.normal);
            crearCell(row, 2, texto(cliente.getRol()), styles.normal);
        }

        autoSize(sheet, 3);
    }

    private void crearHojaProductos(
        Workbook workbook,
        ReportStyles styles,
        List<ProductoDTO> productos,
        List<BodegaDTO> stock
    ) {
        Sheet sheet = workbook.createSheet("Productos y stock");
        crearHeader(sheet, styles.header, "ID", "Producto", "Categoria", "Precio", "Stock");
        Map<Long, Integer> stockPorProducto = stock.stream()
            .filter(item -> item.getProducto() != null && item.getProducto().getId() != null)
            .collect(Collectors.toMap(item -> item.getProducto().getId(), item -> numero(item.getStock()), (a, b) -> b));

        int rowIndex = 1;
        for (ProductoDTO producto : productos) {
            Row row = sheet.createRow(rowIndex++);
            crearCell(row, 0, producto.getId(), styles.normal);
            crearCell(row, 1, texto(producto.getNombre()), styles.normal);
            crearCell(row, 2, categoriaProducto(producto), styles.normal);
            crearCell(row, 3, numero(producto.getPrecio()), styles.money);
            crearCell(row, 4, stockPorProducto.getOrDefault(producto.getId(), 0), styles.normal);
        }

        autoSize(sheet, 5);
    }

    private void crearHojaVentasMensuales(Workbook workbook, ReportStyles styles, List<VentaDTO> ventas) {
        Sheet sheet = workbook.createSheet("Ventas mensuales");
        crearHeader(sheet, styles.header, "Mes", "Cantidad de ventas", "Total vendido");

        int[] cantidadPorMes = new int[12];
        double[] totalPorMes = new double[12];

        for (VentaDTO venta : ventas) {
            LocalDate fecha = obtenerFechaVenta(venta);
            if (fecha == null) {
                continue;
            }

            int mes = fecha.getMonthValue() - 1;
            cantidadPorMes[mes] += 1;
            totalPorMes[mes] += numero(venta.getMonto());
        }

        for (int i = 0; i < 12; i++) {
            Row row = sheet.createRow(i + 1);
            String mes = LocalDate.of(LocalDate.now().getYear(), i + 1, 1)
                .getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "CL"));

            crearCell(row, 0, mes, styles.normal);
            crearCell(row, 1, cantidadPorMes[i], styles.normal);
            crearCell(row, 2, totalPorMes[i], styles.money);
        }

        autoSize(sheet, 3);
    }

    private ReportStyles crearEstilos(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);

        CellStyle normal = workbook.createCellStyle();

        CreationHelper creationHelper = workbook.getCreationHelper();
        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(creationHelper.createDataFormat().getFormat("$#,##0"));

        CellStyle date = workbook.createCellStyle();
        date.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));

        return new ReportStyles(header, normal, money, date);
    }

    private void crearHeader(Sheet sheet, CellStyle style, String... headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            crearCell(row, i, headers[i], style);
        }
    }

    private int agregarResumen(Sheet sheet, ReportStyles styles, int rowIndex, String label, Object value, boolean moneyValue) {
        Row row = sheet.createRow(rowIndex);
        crearCell(row, 0, label, styles.normal);
        crearCell(row, 1, value, moneyValue ? styles.money : styles.normal);
        return rowIndex + 1;
    }

    private void crearCell(Row row, int index, Object value, CellStyle style) {
        Cell cell = row.createCell(index);

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(java.sql.Date.valueOf(date));
        } else {
            cell.setCellValue(texto(value));
        }

        cell.setCellStyle(style);
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private double calcularTotalDinero(List<VentaDTO> ventas) {
        return ventas.stream()
            .map(VentaDTO::getMonto)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .sum();
    }

    private int calcularStockTotal(List<BodegaDTO> stock) {
        return stock.stream()
            .map(BodegaDTO::getStock)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    }

    private LocalDate obtenerFechaVenta(VentaDTO venta) {
        if (venta.getFecha() != null) {
            return venta.getFecha();
        }

        if (venta.getDetalles() == null) {
            return null;
        }

        return venta.getDetalles().stream()
            .map(DetalleVentaDTO::getFecha)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private String productosVenta(VentaDTO venta) {
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return "";
        }

        return venta.getDetalles().stream()
            .map(detalle -> {
                ProductoDTO producto = detalle.getProducto();
                String nombre = producto != null ? texto(producto.getNombre()) : "Producto";
                int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 1;
                return nombre + " x" + cantidad;
            })
            .collect(Collectors.joining(", "));
    }

    private String nombreCliente(ClienteDTO cliente) {
        if (cliente == null) {
            return "";
        }

        return (texto(cliente.getNombres()) + " " + texto(cliente.getApellidos())).trim();
    }

    private Long idCliente(ClienteDTO cliente) {
        return cliente == null ? null : cliente.getId();
    }

    private String categoriaProducto(ProductoDTO producto) {
        if (producto == null || producto.getCategoria() == null) {
            return "";
        }

        return texto(producto.getCategoria().getCategoria());
    }

    private String texto(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int numero(Integer value) {
        return value == null ? 0 : value;
    }

    private double numero(Double value) {
        return value == null ? 0 : value;
    }

    private record ReportStyles(CellStyle header, CellStyle normal, CellStyle money, CellStyle date) {}
}
