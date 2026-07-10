package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Pedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);
}
