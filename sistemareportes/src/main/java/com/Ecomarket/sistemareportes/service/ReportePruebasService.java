package com.Ecomarket.sistemareportes.service;

import com.Ecomarket.sistemareportes.client.ClientesClient;
import com.Ecomarket.sistemareportes.client.InventarioClient;
import com.Ecomarket.sistemareportes.client.VentasClient;
import com.Ecomarket.sistemareportes.dto.ResumenDTO;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportePruebasService {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private VentasClient ventasClient;

    @Autowired
    private InventarioClient inventarioClient;

    @Autowired
    private ClientesClient clientesClient;

    @Autowired
    private InfraestructuraPruebasService infraestructuraPruebasService;

    @Autowired
    private CoberturaCodigoService coberturaCodigoService;

    public String generarReportePruebas() {
        StringBuilder reporte = new StringBuilder();
        ResultadoPruebas resultado = new ResultadoPruebas();

        reporte.append("ECOMARKET - RESULTADO DE PRUEBAS\n");
        reporte.append("Fecha de ejecucion: ").append(LocalDateTime.now()).append("\n\n");

        reporte.append("Infraestructura:\n");

        ejecutarPrueba(reporte, resultado, "Conexion MySQL", infraestructuraPruebasService::probarConexionMysql);
        ejecutarPrueba(reporte, resultado, "API REST puerto 8080", infraestructuraPruebasService::probarApiRest);

        reporte.append("\nServicios de negocio:\n");

        ejecutarPrueba(reporte, resultado, "API clientes", () -> {
            int total = clientesClient.obtenerTodosLosClientes().size();
            return "Se obtuvieron " + total + " usuarios.";
        });

        ejecutarPrueba(reporte, resultado, "API productos", () -> {
            int total = inventarioClient.obtenerTodosLosProductos().size();
            return "Se obtuvieron " + total + " productos.";
        });

        ejecutarPrueba(reporte, resultado, "API stock producto", () -> {
            int total = inventarioClient.obtenerTodoElStock().size();
            return "Se obtuvieron " + total + " registros de stock.";
        });

        ejecutarPrueba(reporte, resultado, "API ventas completas", () -> {
            int total = ventasClient.obtenerTodasLasVentas().size();
            return "Se obtuvieron " + total + " ventas.";
        });

        ejecutarPrueba(reporte, resultado, "Resumen dashboard", () -> {
            ResumenDTO resumen = reporteService.obtenerResumen();
            if (resumen.getTotalVentas() < 0 || resumen.getTotalProductos() < 0 || resumen.getTotalDinero() < 0) {
                throw new IllegalStateException("El resumen contiene valores negativos.");
            }

            return "Ventas=" + resumen.getTotalVentas()
                + ", productos=" + resumen.getTotalProductos()
                + ", total dinero=" + resumen.getTotalDinero() + ".";
        });

        ejecutarPrueba(reporte, resultado, "Generacion Excel", () -> {
            byte[] excel = reporteService.generarReporteExcel();
            if (excel.length == 0) {
                throw new IllegalStateException("El archivo Excel generado esta vacio.");
            }

            return "Excel generado correctamente (" + excel.length + " bytes).";
        });

        reporte.append("\nCobertura de codigo:\n");

        ejecutarPrueba(reporte, resultado, "Cobertura JaCoCo", coberturaCodigoService::generarResumenCobertura);

        reporte.append("\nResumen:\n");
        reporte.append("Pruebas ejecutadas: ").append(resultado.total).append("\n");
        reporte.append("Pruebas correctas: ").append(resultado.correctas).append("\n");
        reporte.append("Pruebas fallidas: ").append(resultado.fallidas).append("\n");
        reporte.append("Estado final: ").append(resultado.fallidas == 0 ? "OK" : "CON ERRORES").append("\n");

        return reporte.toString();
    }

    private void ejecutarPrueba(
        StringBuilder reporte,
        ResultadoPruebas resultado,
        String nombre,
        Supplier<String> prueba
    ) {
        resultado.total++;

        try {
            String detalle = prueba.get();
            resultado.correctas++;
            reporte.append("[OK] ").append(nombre).append(" - ").append(detalle).append("\n");
        } catch (Exception ex) {
            resultado.fallidas++;
            reporte.append("[ERROR] ")
                .append(nombre)
                .append(" - ")
                .append(ex.getClass().getSimpleName())
                .append(": ")
                .append(ex.getMessage())
                .append("\n");
        }
    }

    private static class ResultadoPruebas {
        private int total;
        private int correctas;
        private int fallidas;
    }
}
