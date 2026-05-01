package com.ecomarket.frontend.dto;

import lombok.Data;

@Data
public class ProductoDTO {
    private Long id;
    private String nombre;
    private Double precio;
    private CategoriaDTO categoria;
}