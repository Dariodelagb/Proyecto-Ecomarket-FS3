package com.ecomarket.db.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "direccion")
@Data
public class Direccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String calle;
    private String numero;
    private String comuna;
    private String ciudad;
    private String region;
    private String referencia;
    private Boolean principal;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Cliente cliente;
}
