package com.pedidos360.sistemareportes.client;

import com.pedidos360.sistemareportes.dto.ClienteDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "clientes-service", url = "http://db:8080/api")
public interface ClientesClient {

    @GetMapping("/clientes")
    List<ClienteDTO> obtenerTodosLosClientes();
}
