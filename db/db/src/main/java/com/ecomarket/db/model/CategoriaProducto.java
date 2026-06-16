package com.ecomarket.db.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categoria_producto")
@Data
public class CategoriaProducto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String categoria;
}
