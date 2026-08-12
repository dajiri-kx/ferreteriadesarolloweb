package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "PAGO")
@Data
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Column(name = "metodo", nullable = false, length = 30)
    private String metodo; // 'tarjeta','transferencia_sinpe','paypal','stripe'

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "pendiente"; // 'pendiente','completado','fallido','reembolsado'

    @Column(name = "referencia_gateway", length = 120)
    private String referenciaGateway;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
