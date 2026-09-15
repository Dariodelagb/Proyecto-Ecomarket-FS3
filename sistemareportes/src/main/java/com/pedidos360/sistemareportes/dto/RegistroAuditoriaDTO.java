package com.pedidos360.sistemareportes.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RegistroAuditoriaDTO {
    private Long id;
    private LocalDateTime fecha;
    private String accion;
    private String entidad;
    private Long entidadId;
    private Long actorId;
    private String actorNombre;
    private String actorRol;
    private String detalle;
}
