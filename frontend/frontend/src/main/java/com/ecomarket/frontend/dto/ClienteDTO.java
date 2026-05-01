package com.ecomarket.frontend.dto;

import lombok.Data;

@Data
public class ClienteDTO {
    private Long id;
    private String nombres;
    private String apellidos;
    private Integer rut;
    private String dvrut;
}
