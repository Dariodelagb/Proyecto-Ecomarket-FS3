package com.Ecomarket.sistemareportes.dto;

import lombok.Data;

@Data
public class ProductoDTO {
    private Long id;
    private String nombre;
    private Integer precio;
    private String categoriaNombre; // Para simplificar, solo el nombre de la categoria
}