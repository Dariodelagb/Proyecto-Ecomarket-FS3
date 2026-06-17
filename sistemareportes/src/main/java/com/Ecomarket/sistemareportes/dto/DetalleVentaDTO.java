package com.Ecomarket.sistemareportes.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class DetalleVentaDTO {
    private Long id;
    private Integer cantidad;
    private Double precioUnitario;
    private LocalDate fecha;
    private ProductoDTO producto;
}
