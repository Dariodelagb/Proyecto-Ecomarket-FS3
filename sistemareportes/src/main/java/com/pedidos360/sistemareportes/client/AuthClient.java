package com.pedidos360.sistemareportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.pedidos360.sistemareportes.dto.SesionClienteDTO;

@FeignClient(name = "auth-service", url = "http://db:8080/api")
public interface AuthClient {
    @GetMapping("/auth/sesion/{token}")
    SesionClienteDTO obtenerSesion(@PathVariable("token") String token);
}
