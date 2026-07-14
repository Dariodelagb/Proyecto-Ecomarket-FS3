package com.ecomarket.db.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CarritoProductoId implements Serializable {
    private Long carritoId;
    private Long productoId;
}
