package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "CARRITO")
@Data
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDate fechaCreacion;
}
