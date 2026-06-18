package com.Ecomarket.sistemareportes.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class VentaDTO {
    private Long id;
    private Integer cantidad;
    private String tipoEnvio;
    private Double monto;
    private LocalDate fecha;
    private ClienteDTO cliente;
    private DireccionDTO direccion;
    private List<DetalleVentaDTO> detalles;
}
