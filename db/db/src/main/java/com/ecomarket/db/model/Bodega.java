package com.ecomarket.db.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Bodega {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer stock;
    
    @OneToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
}