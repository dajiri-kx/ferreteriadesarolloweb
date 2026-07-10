/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.proyecto.toolboxcr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 *
 * @author edwua
 */
@Data
@Entity
@Table(name = "PRODUCTO")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "sku", nullable = false, unique = true, length = 40)
    private String sku;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precio", nullable = false)
    private BigDecimal precio;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "marca", length = 80)
    private String marca;

    @Column(name = "material", length = 80)
    private String material;

    @Column(name = "dimensiones", length = 80)
    private String dimensiones;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "precio_oferta")
    private BigDecimal precioOferta;

    @Column(name = "oferta_fecha_inicio")
    private LocalDate ofertaFechaInicio;

    @Column(name = "oferta_fecha_fin")
    private LocalDate ofertaFechaFin;

    @Column(name = "porcentaje_descuento", insertable = false, updatable = false)
    private BigDecimal porcentajeDescuento;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
