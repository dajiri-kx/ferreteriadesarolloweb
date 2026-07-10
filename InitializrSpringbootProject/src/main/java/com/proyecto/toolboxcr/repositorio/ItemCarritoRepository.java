package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Carrito;
import com.proyecto.toolboxcr.domain.ItemCarrito;
import com.proyecto.toolboxcr.domain.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    List<ItemCarrito> findByCarrito(Carrito carrito);

    Optional<ItemCarrito> findByCarritoAndProducto(Carrito carrito, Producto producto);

    void deleteByCarrito(Carrito carrito);
}
