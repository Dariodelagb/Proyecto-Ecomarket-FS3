package com.Ecomarket.sistemareportes.controller;

import com.Ecomarket.sistemareportes.dto.ResumenDTO;
import com.Ecomarket.sistemareportes.service.ReportePruebasService;
import com.Ecomarket.sistemareportes.service.ReporteService;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private ReportePruebasService reportePruebasService;

    @GetMapping("/api/reportes/generar")
    public String generarReporte() {
        reporteService.procesarReporte();
        return "Reporte generado. Revisa la consola para los detalles.";
    }

    @GetMapping("/api/reportes/resumen")
    public ResumenDTO obtenerResumen() {
        return reporteService.obtenerResumen();
    }

    @GetMapping({"/get-reporte", "/api/reportes/get-reporte"})
    public ResponseEntity<byte[]> descargarReporte() {
        byte[] excel = reporteService.generarReporteExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("reporte-dashboard.xlsx")
                .build()
        );
        headers.setContentLength(excel.length);

        return ResponseEntity.ok()
            .headers(headers)
            .body(excel);
    }

    @GetMapping({"/get-pruebas", "/api/reportes/get-pruebas"})
    public ResponseEntity<byte[]> descargarReportePruebas() {
        byte[] contenido = reportePruebasService.generarReportePruebas().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("resultado-pruebas.txt")
                .build()
        );
        headers.setContentLength(contenido.length);

        return ResponseEntity.ok()
            .headers(headers)
            .body(contenido);
    }
}
