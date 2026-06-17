package com.Ecomarket.sistemareportes.dto;

import lombok.Data;

@Data
public class DireccionDTO {
    private Long id;
    private String calle;
    private String numero;
    private String comuna;
    private String ciudad;
    private String region;
    private String referencia;
    private Boolean principal;
}
