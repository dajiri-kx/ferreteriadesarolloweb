package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "PRODUCTO_IMAGEN")
public class ProductoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "url_imagen", nullable = false, length = 255)
    private String urlImagen;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;
}