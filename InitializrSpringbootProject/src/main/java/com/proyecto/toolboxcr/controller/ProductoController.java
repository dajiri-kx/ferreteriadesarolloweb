package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Categoria;
import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.service.CategoriaService;
import com.proyecto.toolboxcr.service.InventarioService;
import com.proyecto.toolboxcr.service.ProductoService;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author edwua
 */
@Controller
@RequestMapping("/producto")
public class ProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final InventarioService inventarioService;

    public ProductoController(ProductoService productoService,
            CategoriaService categoriaService,
            InventarioService inventarioService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.inventarioService = inventarioService;
    }

    @GetMapping("/listado")
    public String listado(Model model) {
        var productos = productoService.listarProductos();
        model.addAttribute("productos", productos);
        model.addAttribute("totalProductos", productos.size());
        return "producto/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "producto/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto,
            @RequestParam("categoriaId") Long categoriaId,
            @RequestParam(required = false) MultipartFile imagenFile,
            @RequestParam(required = false, defaultValue = "0") Integer stockInicial,
            @RequestParam(required = false, defaultValue = "5") Integer umbralInicial,
            RedirectAttributes redirectAttrs) {

        Categoria categoria = categoriaService.obtenerPorId(categoriaId);
        if (categoria == null) {
            redirectAttrs.addFlashAttribute("error", "Debe seleccionar una categoría válida.");
            return "redirect:/producto/nuevo";
        }

        boolean esNuevo = (producto.getId() == null);
        producto.setCategoria(categoria);
        producto.setActivo(true);
        productoService.guardar(producto, imagenFile);

        if (esNuevo) {
            inventarioService.inicializarInventario(producto, stockInicial, umbralInicial);
        }

        redirectAttrs.addFlashAttribute("todoOk", "El producto se guardó correctamente.");
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

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam Long id, RedirectAttributes redirectAttrs) {
        try {
            productoService.eliminar(id);
            redirectAttrs.addFlashAttribute("todoOk", "El producto se eliminó correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/producto/listado";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        var producto = productoService.obtenerProducto(id);
        if (producto.isPresent()) {
            model.addAttribute("producto", producto.get());
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "producto/formulario";
        }
        return "redirect:/producto/listado";
    }

    // A-01, criterio de aceptación 2: carga masiva desde CSV
    @PostMapping("/importarCsv")
    public String importarCsv(@RequestParam MultipartFile archivoCsv, RedirectAttributes redirectAttrs) {
        try {
            var resultado = productoService.importarCsv(archivoCsv);
            redirectAttrs.addFlashAttribute("todoOk",
                    resultado.exitosos + " producto(s) importado(s) correctamente.");
            if (!resultado.errores.isEmpty()) {
                redirectAttrs.addFlashAttribute("error",
                        resultado.errores.size() + " fila(s) con problemas: "
                        + String.join(" | ", resultado.errores));
            }
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("error", "No se pudo leer el archivo CSV.");
        } catch (com.opencsv.exceptions.CsvException e) {
            redirectAttrs.addFlashAttribute("error", "El archivo CSV tiene un formato inválido.");
        }
        return "redirect:/producto/listado";

    }
    

}
