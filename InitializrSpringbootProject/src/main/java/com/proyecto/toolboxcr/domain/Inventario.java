package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "INVENTARIO")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "producto_id", unique = true)
    private Producto producto;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(name = "umbral_minimo", nullable = false)
    private Integer umbralMinimo;

    @Column(name = "motivo_ajuste", length = 255)
    private String motivoAjuste;

    // La BD la llena sola con DEFAULT/ON UPDATE CURRENT_TIMESTAMP
    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;
}