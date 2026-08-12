package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Pago;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByPedidoId(Long pedidoId);
}
