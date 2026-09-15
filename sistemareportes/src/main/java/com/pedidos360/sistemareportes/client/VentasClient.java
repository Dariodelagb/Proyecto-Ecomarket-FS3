package com.pedidos360.sistemareportes.client;

import com.pedidos360.sistemareportes.dto.VentaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "ventas-service", url = "http://db:8080/api")
public interface VentasClient {

    @GetMapping("/ventas-completas")
    List<VentaDTO> obtenerTodasLasVentas();
}