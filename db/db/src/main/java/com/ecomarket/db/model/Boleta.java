package com.ecomarket.db.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.*;

@Entity
@Data
public class Boleta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer monto;
    private LocalDate fecha;
    
    @OneToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;
}