package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);

    List<Pedido> findAllByOrderByFechaDesc();

    List<Pedido> findByEstadoOrderByFechaDesc(String estado);

    List<Pedido> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Pedido> findByEstadoAndFechaBetweenOrderByFechaDesc(String estado, LocalDateTime inicio, LocalDateTime fin);
}
