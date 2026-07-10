package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Inventario;
import com.proyecto.toolboxcr.domain.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByProducto(Producto producto);

    Optional<Inventario> findByProducto_Id(Long idProducto);

    // productos cuyo stock ya está en o bajo el umbral configurado.
    @Query("SELECT i FROM Inventario i WHERE i.stockDisponible <= i.umbralMinimo")
    List<Inventario> consultaStockBajo();
}