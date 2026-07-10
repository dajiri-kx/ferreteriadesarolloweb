package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "DIRECCION_ENVIO")
@Data
public class DireccionEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @Size(max = 60)
    private String alias;

    @NotNull
    @Size(max = 255)
    private String direccion;

    @NotNull
    @Size(max = 15)
    @Column(name = "codigo_postal")
    private String codigoPostal;

    @Column(name = "es_predeterminada")
    private Boolean esPredeterminada;
}
