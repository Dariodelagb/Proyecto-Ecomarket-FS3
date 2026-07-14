package com.ecomarket.db.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "carrito_producto")
@Data
public class CarritoProducto {
    @EmbeddedId
    private CarritoProductoId id = new CarritoProductoId();

    @ManyToOne
    @MapsId("carritoId")
    @JoinColumn(name = "carrito_id")
    @JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Carrito carrito;

    @ManyToOne
    @MapsId("productoId")
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad = 1;
}
