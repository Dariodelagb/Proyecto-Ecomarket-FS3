package com.pedidos360.sistemareportes.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.pedidos360.sistemareportes.dto.RegistroAuditoriaDTO;

@FeignClient(name = "auditoria-service", url = "http://db:8080/api")
public interface AuditoriaClient {
    @GetMapping("/auditoria/reporte")
    List<RegistroAuditoriaDTO> obtenerRegistros(@RequestHeader("X-Report-Key") String reportKey);
}
