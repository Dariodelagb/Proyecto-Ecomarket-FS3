package com.Ecomarket.sistemareportes.dto;

import lombok.Data;

@Data
public class ProductoDTO {
    private Long id;
    private String nombre;
    private Integer precio;
    private CategoriaDTO categoria;
}
