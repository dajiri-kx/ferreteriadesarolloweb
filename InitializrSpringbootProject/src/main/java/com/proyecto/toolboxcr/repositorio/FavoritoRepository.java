package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Favorito;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.domain.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByCliente(Usuario cliente);
    Optional<Favorito> findByClienteAndProducto(Usuario cliente, Producto producto);
    boolean existsByClienteAndProducto(Usuario cliente, Producto producto);
}
