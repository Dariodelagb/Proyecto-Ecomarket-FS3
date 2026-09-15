package com.pedidos360.sistemareportes.dto;

import lombok.Data;

@Data
public class BodegaDTO {
    private Long id;
    private Integer stock;
    private ProductoDTO producto;
}