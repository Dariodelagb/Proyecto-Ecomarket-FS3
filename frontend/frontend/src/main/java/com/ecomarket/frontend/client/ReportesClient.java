package com.ecomarket.frontend.client;

import com.ecomarket.frontend.dto.ResumenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "reportes-service", url = "http://sistemareportes:8082/api/reportes")
public interface ReportesClient {

    @GetMapping("/resumen")
    ResumenDTO obtenerResumen();
}