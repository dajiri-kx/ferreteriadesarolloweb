package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "ALERTA_INVENTARIO")
public class AlertaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "stock_al_momento", insertable = false, updatable = false)
    private Integer stockAlMomento;

    @Column(name = "umbral_minimo", insertable = false, updatable = false)
    private Integer umbralMinimo;

    @Column(name = "fecha_alerta", insertable = false, updatable = false)
    private LocalDateTime fechaAlerta;

    @Column(name = "atendida", nullable = false)
    private boolean atendida;
}