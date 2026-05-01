package com.ecomarket.frontend.service;

import com.ecomarket.frontend.dto.ProductoDTO;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

// En tu proyecto de Frontend (Thymeleaf)
@Service
public class ProductoService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8080/api/productos";

    public List<ProductoDTO> obtenerProductos() {
        // Llama al backend y trae la lista
        ProductoDTO[] response = restTemplate.getForObject(API_URL, ProductoDTO[].class);
        return Arrays.asList(response);
    }
}