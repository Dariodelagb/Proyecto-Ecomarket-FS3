package com.Ecomarket.sistemareportes.controller;

import com.Ecomarket.sistemareportes.dto.ResumenDTO;
import com.Ecomarket.sistemareportes.service.ReporteService;
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
}
