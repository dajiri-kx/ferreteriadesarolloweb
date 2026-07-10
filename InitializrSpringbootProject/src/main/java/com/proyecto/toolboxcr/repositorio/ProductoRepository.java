package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
