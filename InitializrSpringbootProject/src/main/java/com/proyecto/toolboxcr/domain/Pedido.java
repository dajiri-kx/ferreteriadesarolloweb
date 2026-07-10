package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Entity
@Table(name = "PEDIDO")
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_orden")
    private String numeroOrden;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    private LocalDateTime fecha;

    private String estado;

    private BigDecimal subtotal;

    @Column(name = "costo_envio")
    private BigDecimal costoEnvio;

    @Column(name = "descuento_total")
    private BigDecimal descuentoTotal;

    private BigDecimal total;

    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY)
    private List<DetallePedido> detalles = new ArrayList<>();
}
