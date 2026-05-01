package com.Ecomarket.sistemareportes.service;

import com.Ecomarket.sistemareportes.client.VentasClient;
import com.Ecomarket.sistemareportes.dto.VentaDTO;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private VentasClient ventasClient;

    public void procesarReporte() {
        // Se llama al otro microservicio como si fuera un método local
        List<VentaDTO> listaVentas = ventasClient.obtenerTodasLasVentas();
        
        // Aquí va la salida del reporte
        System.out.println("Ventas recibidas: " + listaVentas.size());
    }
}