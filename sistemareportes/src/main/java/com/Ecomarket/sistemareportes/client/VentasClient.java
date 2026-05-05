package com.Ecomarket.sistemareportes.client;

import com.Ecomarket.sistemareportes.dto.VentaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "ventas-service", url = "http://db:8080/api")
public interface VentasClient {

    @GetMapping("/ventas-completas")
    List<VentaDTO> obtenerTodasLasVentas();
}