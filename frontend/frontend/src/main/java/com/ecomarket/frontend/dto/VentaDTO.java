package com.ecomarket.frontend.dto;

import java.util.List;

import lombok.Data;

@Data
public class VentaDTO {
    private Long id;
    private String tipoEnvio;
    private Double monto;
    private ClienteDTO cliente;
    private List<DetalleVentaDTO> detalles;
}