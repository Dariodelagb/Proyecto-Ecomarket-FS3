package com.ecomarket.db.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "carrito")
@Data
public class Carrito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarritoProducto> items = new ArrayList<>();

    @Transient
    public List<Producto> getProductos() {
        List<Producto> productos = new ArrayList<>();

        for (CarritoProducto item : items) {
            int cantidad = item.getCantidad() == null ? 1 : item.getCantidad();
            for (int i = 0; i < cantidad; i++) {
                productos.add(item.getProducto());
            }
        }

        return productos;
    }
}
