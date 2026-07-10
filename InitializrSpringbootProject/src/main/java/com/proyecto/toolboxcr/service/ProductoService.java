/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.repository.ProductoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author edwua
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosActivos() {
        return productoRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Producto> obtenerProducto(Long id) {
        return productoRepository.findById(id);
    }

    @Transactional
    public void guardar(Producto producto) {
        productoRepository.save(producto);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Optional<Producto> producto = productoRepository.findById(id);

        if (producto.isPresent()) {
            Producto p = producto.get();
            p.setActivo(activo);
            productoRepository.save(p);
        }
    }
}
