package com.Ecomarket.sistemareportes.dto;

import java.util.List;
import lombok.Data;

@Data
public class ClienteDTO {
    private Long id;
    private String nombres;
    private String apellidos;
    private Integer rut;
    private String dvrut;
    private String email;
    private String rol;
    private List<DireccionDTO> direcciones;
}
