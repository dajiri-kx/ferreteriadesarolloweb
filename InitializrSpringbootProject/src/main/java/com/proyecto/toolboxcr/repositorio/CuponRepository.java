package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {
    Optional<Cupon> findByCodigoIgnoreCase(String codigo);
}
