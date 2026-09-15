package com.pedidos360.frontend.dto;

import lombok.Data;

@Data
public class DetalleVentaDTO {
    private Long id;

    private Integer cantidad;
    private Double precioUnitario;

    private ProductoDTO producto;

    private VentaDTO venta;
}
