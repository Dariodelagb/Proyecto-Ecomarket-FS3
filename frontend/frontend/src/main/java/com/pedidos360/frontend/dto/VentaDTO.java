package com.pedidos360.frontend.dto;

import java.util.List;
import java.time.LocalDate;

import lombok.Data;

@Data
public class VentaDTO {
    private Long id;
    private String tipoEnvio;
    private Double monto;
    private LocalDate fecha;
    private String estado;
    private ClienteDTO cliente;
    private List<DetalleVentaDTO> detalles;
}
