package com.Ecomarket.sistemareportes.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.Ecomarket.sistemareportes.client.ClientesClient;
import com.Ecomarket.sistemareportes.client.InventarioClient;
import com.Ecomarket.sistemareportes.client.VentasClient;
import com.Ecomarket.sistemareportes.dto.ClienteDTO;
import com.Ecomarket.sistemareportes.dto.ProductoDTO;
import com.Ecomarket.sistemareportes.dto.ResumenDTO;
import com.Ecomarket.sistemareportes.dto.VentaDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportePruebasServiceTest {

    @Mock
    private ReporteService reporteService;

    @Mock
    private VentasClient ventasClient;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private ClientesClient clientesClient;

    @Mock
    private InfraestructuraPruebasService infraestructuraPruebasService;

    @Mock
    private CoberturaCodigoService coberturaCodigoService;

    @InjectMocks
    private ReportePruebasService reportePruebasService;

    @Test
    void generarReportePruebasDevuelveTextoConResumenOk() {
        ResumenDTO resumen = new ResumenDTO();
        resumen.setTotalDinero(10000.0);
        resumen.setTotalVentas(1);
        resumen.setTotalProductos(1);

        when(clientesClient.obtenerTodosLosClientes()).thenReturn(List.of(new ClienteDTO()));
        when(inventarioClient.obtenerTodosLosProductos()).thenReturn(List.of(new ProductoDTO()));
        when(inventarioClient.obtenerTodoElStock()).thenReturn(List.of());
        when(ventasClient.obtenerTodasLasVentas()).thenReturn(List.of(new VentaDTO()));
        when(infraestructuraPruebasService.probarConexionMysql()).thenReturn("Conexion MySQL OK.");
        when(infraestructuraPruebasService.probarApiRest()).thenReturn("API REST OK.");
        when(reporteService.obtenerResumen()).thenReturn(resumen);
        when(reporteService.generarReporteExcel()).thenReturn(new byte[] {1, 2, 3});
        when(coberturaCodigoService.generarResumenCobertura()).thenReturn("Lineas=75.00%.");

        String reporte = reportePruebasService.generarReportePruebas();

        assertTrue(reporte.contains("ECOMARKET - RESULTADO DE PRUEBAS"));
        assertTrue(reporte.contains("[OK] Conexion MySQL"));
        assertTrue(reporte.contains("[OK] API REST puerto 8080"));
        assertTrue(reporte.contains("[OK] API clientes"));
        assertTrue(reporte.contains("[OK] Generacion Excel"));
        assertTrue(reporte.contains("[OK] Cobertura JaCoCo"));
        assertTrue(reporte.contains("Estado final: OK"));
    }
}
