package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.AlertaInventario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertaInventarioRepository extends JpaRepository<AlertaInventario, Long> {

    // Consulta derivada: alertas que el encargado de bodega aún no revisó
    List<AlertaInventario> findByAtendidaFalseOrderByFechaAlertaDesc();

    List<AlertaInventario> findAllByOrderByFechaAlertaDesc();
}