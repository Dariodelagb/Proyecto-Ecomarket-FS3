package com.Ecomarket.sistemareportes.controller;

import com.Ecomarket.sistemareportes.dto.ResumenDTO;
import com.Ecomarket.sistemareportes.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/generar")
    public String generarReporte() {
        reporteService.procesarReporte();
        return "Reporte generado. Revisa la consola para los detalles.";
    }

    @GetMapping("/resumen")
    public ResumenDTO obtenerResumen() {
        return reporteService.obtenerResumen();
    }
}