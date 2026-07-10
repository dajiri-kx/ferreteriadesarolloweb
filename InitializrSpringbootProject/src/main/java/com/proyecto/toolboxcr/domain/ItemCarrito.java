package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;

@Entity
@Table(name = "ITEM_CARRITO")
@Data
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    /* Calculado en Java para los templates (no es columna en BD) */
    @Transient
    public BigDecimal getSubtotal() {
        if (producto == null || cantidad == null) return BigDecimal.ZERO;
        return producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }
}
