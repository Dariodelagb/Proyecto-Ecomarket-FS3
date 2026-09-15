package com.pedidos360.db.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "venta")
@Data
public class Venta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoEnvio;
    private Double monto;
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado = EstadoPedido.CREADO;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "direccion_id")
    private Direccion direccion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles;

    @PrePersist
    public void prePersist() {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        if (estado == null) {
            estado = EstadoPedido.CREADO;
        }
    }
}
