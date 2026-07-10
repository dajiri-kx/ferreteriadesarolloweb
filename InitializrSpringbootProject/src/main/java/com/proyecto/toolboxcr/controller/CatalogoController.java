package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.repositorio.InventarioRepository;
import com.proyecto.toolboxcr.repositorio.ProductoImagenRepository;
import com.proyecto.toolboxcr.service.ProductoService;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogoController {

    private final ProductoService productoService;
    private final InventarioRepository inventarioRepository;
    private final ProductoImagenRepository productoImagenRepository;

    public CatalogoController(ProductoService productoService,
            InventarioRepository inventarioRepository,
            ProductoImagenRepository productoImagenRepository) {
        this.productoService = productoService;
        this.inventarioRepository = inventarioRepository;
        this.productoImagenRepository = productoImagenRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /* Catálogo general, con filtro opcional por categoría (?categoria=herramientas) */
    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(required = false) String categoria, Model model) {
        List<Producto> resultados;
        if (categoria != null && !categoria.isBlank()) {
            String norm = normalizar(categoria);
            resultados = productoService.listarProductosActivos().stream()
                    .filter(p -> p.getCategoria() != null
                            && normalizar(p.getCategoria().getNombre()).contains(norm))
                    .collect(Collectors.toList());
        } else {
            resultados = productoService.listarProductosActivos();
        }
        model.addAttribute("resultados", resultados);
        model.addAttribute("totalResultados", resultados.size());
        model.addAttribute("q", categoria);
        return "index";
    }

    private String normalizar(String s) {
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase();
    }

    @GetMapping("/catalogo/buscar")
    public String buscar(@RequestParam(required = false) String q, Model model) {
        if (q != null && !q.isBlank()) {
            var resultados = productoService.buscar(q);
            model.addAttribute("resultados", resultados);
            model.addAttribute("totalResultados", resultados.size());
        }
        model.addAttribute("q", q);
        return "index";
    }

    @GetMapping("/catalogo/producto/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        var productoOpt = productoService.obtenerProducto(id);
        if (productoOpt.isEmpty()) {
            return "redirect:/";
        }
        Producto producto = productoOpt.get();
        model.addAttribute("producto", producto);

        var inventarioOpt = inventarioRepository.findByProducto(producto);
        model.addAttribute("inventario", inventarioOpt.orElse(null));

        var imagenes = productoImagenRepository.findByProducto(producto);
        model.addAttribute("imagenes", imagenes);

        if (producto.getCategoria() != null) {
            var relacionados = productoService.listarPorCategoria(
                    producto.getCategoria().getId())
                    .stream()
                    .filter(p -> !p.getId().equals(id))
                    .limit(3)
                    .collect(Collectors.toList());
            model.addAttribute("relacionados", relacionados);
        }

        return "catalogo/detalle";
    }
}