package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.domain.ProductoImagen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Long> {

    List<ProductoImagen> findByProducto(Producto producto);

    // Consulta derivada: trae la imagen marcada como principal de un producto
    ProductoImagen findByProductoAndEsPrincipalTrue(Producto producto);
}