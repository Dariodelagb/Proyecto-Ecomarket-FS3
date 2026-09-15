package com.pedidos360.sistemareportes.dto;

import lombok.Data;

@Data
public class SesionClienteDTO {
    private String token;
    private ClienteDTO cliente;
}
