package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.DetallePedido;
import com.proyecto.toolboxcr.domain.Pedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    List<DetallePedido> findByPedido(Pedido pedido);
}
