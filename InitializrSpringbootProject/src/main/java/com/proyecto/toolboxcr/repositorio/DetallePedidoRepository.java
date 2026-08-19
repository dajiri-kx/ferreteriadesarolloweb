package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.DetallePedido;
import com.proyecto.toolboxcr.domain.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    List<DetallePedido> findByPedido(Pedido pedido);

    @Query("""
       SELECT d.producto.nombre,
              SUM(d.cantidad),
              SUM(d.precioUnitario * d.cantidad)
       FROM DetallePedido d
       WHERE d.pedido.fecha BETWEEN :inicio AND :fin
       AND d.pedido.estado <> 'cancelado'
       GROUP BY d.producto.id, d.producto.nombre
       ORDER BY SUM(d.cantidad) DESC
       """)
    List<Object[]> rankingProductosVendidos(@Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);
}
