package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;

@Entity
@Table(name = "METODO_ENVIO")
@Data
public class MetodoEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "costo_base", nullable = false)
    private BigDecimal costoBase;

    @Column(name = "tiempo_estimado_dias", nullable = false)
    private Integer tiempoEstimadoDias;

    @Column(name = "requiere_direccion", nullable = false)
    private Boolean requiereDireccion;
}
