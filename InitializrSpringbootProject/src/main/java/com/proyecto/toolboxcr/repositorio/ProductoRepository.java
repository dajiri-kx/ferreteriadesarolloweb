/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

