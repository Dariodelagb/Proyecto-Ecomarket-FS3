package com.ecomarket.db.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cliente")
@Data
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombres;
    private String apellidos;
    private Integer rut;
    private String dvrut;
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<Direccion> direcciones;
}
