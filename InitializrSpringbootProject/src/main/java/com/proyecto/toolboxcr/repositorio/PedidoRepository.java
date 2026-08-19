package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);

    List<Pedido> findAllByOrderByFechaDesc();

    List<Pedido> findByEstadoOrderByFechaDesc(String estado);

    List<Pedido> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Pedido> findByEstadoAndFechaBetweenOrderByFechaDesc(String estado, LocalDateTime inicio, LocalDateTime fin);

    @Query("""
       SELECT COALESCE(SUM(p.total), 0)
       FROM Pedido p
       WHERE p.fecha BETWEEN :inicio AND :fin
       AND p.estado <> 'cancelado'
       """)
    BigDecimal sumarVentasPorPeriodo(@Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("""
       SELECT COUNT(p)
       FROM Pedido p
       WHERE p.fecha BETWEEN :inicio AND :fin
       AND p.estado <> 'cancelado'
       """)
    Long contarPedidosPorPeriodo(@Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("""
       SELECT p
       FROM Pedido p
       WHERE p.fecha BETWEEN :inicio AND :fin
       AND p.estado <> 'cancelado'
       ORDER BY p.fecha DESC
       """)
    List<Pedido> buscarPedidosReporte(@Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);
}
