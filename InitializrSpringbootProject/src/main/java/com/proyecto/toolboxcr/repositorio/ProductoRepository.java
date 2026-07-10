/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author edwua
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Producto findBySku(String sku);

    boolean existsBySku(String sku);

    List<Producto> findByActivoTrue();

    // Consulta derivada para buscar por SKU sin importar mayúsculas/minúsculas (carga CSV)
    boolean existsBySkuIgnoreCase(String sku);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND "
            + "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Producto> buscarPorNombreOSkuODescripcion(@Param("q") String q);
}
