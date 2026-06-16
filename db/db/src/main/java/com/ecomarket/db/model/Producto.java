package com.ecomarket.db.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "producto")
@Data
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private Integer precio;
    
    @ManyToOne
    @JoinColumn(name = "categoria_producto_id")
    private CategoriaProducto categoria;
}
