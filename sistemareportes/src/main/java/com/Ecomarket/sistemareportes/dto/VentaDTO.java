package com.Ecomarket.sistemareportes.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VentaDTO {
    private Long id;
    private Integer cantidad;
    private String tipoEnvio;
    private Double monto;
    private LocalDate fecha;
}