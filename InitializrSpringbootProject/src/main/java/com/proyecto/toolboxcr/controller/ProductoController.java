/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author edwua
 */
@Controller
@RequestMapping("/producto")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/listado")
    public String listado(Model model) {
        model.addAttribute("productos", productoService.listarProductos());
        return "producto/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("producto", new Producto());
        return "producto/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto) {
        productoService.guardar(producto);
        return "redirect:/producto/listado";
    }

    @GetMapping("/activar/{id}")
    public String activar(@PathVariable Long id) {
        productoService.cambiarEstado(id, true);
        return "redirect:/producto/listado";
    }

    @GetMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Long id) {
        productoService.cambiarEstado(id, false);
        return "redirect:/producto/listado";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {

        var producto = productoService.obtenerProducto(id);

        if (producto.isPresent()) {
            model.addAttribute("producto", producto.get());
            return "producto/formulario";
        }

        return "redirect:/producto/listado";
    }

}
