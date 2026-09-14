package com.ecomarket.frontend.dto;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class VentaDTO {
    private Long id;
    private String tipoEnvio;
    private Double monto;
    private Date fecha;
    private String estado;
    private ClienteDTO cliente;
    private List<DetalleVentaDTO> detalles;
}