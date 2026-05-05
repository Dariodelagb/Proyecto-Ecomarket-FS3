package com.Ecomarket.sistemareportes.client;

import com.Ecomarket.sistemareportes.dto.BodegaDTO;
import com.Ecomarket.sistemareportes.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "inventario-service", url = "http://db:8080/api")
public interface InventarioClient {

    @GetMapping("/productos")
    List<ProductoDTO> obtenerTodosLosProductos();

    @GetMapping("/bodega")
    List<BodegaDTO> obtenerTodoElStock();
}