package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Carrito;
import com.proyecto.toolboxcr.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByCliente(Usuario cliente);
}
