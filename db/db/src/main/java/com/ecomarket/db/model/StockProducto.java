package com.ecomarket.db.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "stock_producto")
@Data
public class StockProducto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer stock;

    @OneToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
}
