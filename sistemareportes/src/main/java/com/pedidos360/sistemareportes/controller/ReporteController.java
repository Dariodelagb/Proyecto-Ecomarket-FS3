package com.pedidos360.sistemareportes.controller;

import com.pedidos360.sistemareportes.dto.ResumenDTO;
import com.pedidos360.sistemareportes.service.ReportePruebasService;
import com.pedidos360.sistemareportes.service.ReporteService;
import com.pedidos360.sistemareportes.service.ReporteAuthorizationService;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private ReportePruebasService reportePruebasService;

    @Autowired
    private ReporteAuthorizationService reporteAuthorizationService;

    @GetMapping("/api/reportes/generar")
    public String generarReporte(@RequestHeader("X-Session-Token") String token) {
        reporteAuthorizationService.exigirAdministrador(token);
        reporteService.procesarReporte();
        return "Reporte generado. Revisa la consola para los detalles.";
    }

    @GetMapping("/api/reportes/resumen")
    public ResumenDTO obtenerResumen(@RequestHeader("X-Session-Token") String token) {
        reporteAuthorizationService.exigirAdministrador(token);
        return reporteService.obtenerResumen();
    }

    @GetMapping({"/get-reporte", "/api/reportes/get-reporte"})
    public ResponseEntity<byte[]> descargarReporte(
        @RequestHeader("X-Session-Token") String token
    ) {
        reporteAuthorizationService.exigirAdministrador(token);
        byte[] excel = reporteService.generarReporteExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("reporte-pedidos360.xlsx")
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
