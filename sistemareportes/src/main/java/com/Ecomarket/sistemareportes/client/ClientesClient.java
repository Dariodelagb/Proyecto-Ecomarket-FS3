package com.Ecomarket.sistemareportes.client;

import com.Ecomarket.sistemareportes.dto.ClienteDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "clientes-service", url = "http://db:8080/api")
public interface ClientesClient {

    @GetMapping("/clientes")
    List<ClienteDTO> obtenerTodosLosClientes();
}
