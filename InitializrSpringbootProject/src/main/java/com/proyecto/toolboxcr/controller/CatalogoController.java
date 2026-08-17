package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.repositorio.InventarioRepository;
import com.proyecto.toolboxcr.repositorio.ProductoImagenRepository;
import com.proyecto.toolboxcr.service.ProductoService;
import com.proyecto.toolboxcr.service.CategoriaService;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class CatalogoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final InventarioRepository inventarioRepository;
    private final ProductoImagenRepository productoImagenRepository;

    public CatalogoController(ProductoService productoService,
            CategoriaService categoriaService,
            InventarioRepository inventarioRepository,
            ProductoImagenRepository productoImagenRepository) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.inventarioRepository = inventarioRepository;
        this.productoImagenRepository = productoImagenRepository;
    }

    @ModelAttribute("categorias")
    public List<com.proyecto.toolboxcr.domain.Categoria> getCategorias() {
        return categoriaService.listarCategorias();
    }

    @ModelAttribute("ofertas")
    public List<Producto> getOfertas() {
        return productoService.listarProductosActivos().stream()
                .filter(p -> p.getPrecioOferta() != null)
                .collect(Collectors.toList());
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /* Catálogo general, con filtro opcional por categoría (?categoria=herramientas) */
    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(required = false) String categoria,
            @RequestParam(required = false) java.math.BigDecimal precioMin,
            @RequestParam(required = false) java.math.BigDecimal precioMax,
            @RequestParam(required = false) Boolean soloDisponibles,
            Model model) {
        List<Producto> resultados = productoService.listarProductosActivos().stream()
                .filter(p -> categoria == null || categoria.isBlank()
                || (p.getCategoria() != null
                && normalizar(p.getCategoria().getNombre()).contains(normalizar(categoria))))
                .filter(p -> precioMin == null || p.getPrecio().compareTo(precioMin) >= 0)
                .filter(p -> precioMax == null || p.getPrecio().compareTo(precioMax) <= 0)
                .collect(Collectors.toList());

        model.addAttribute("resultados", resultados);
        model.addAttribute("totalResultados", resultados.size());
        model.addAttribute("categoriaActual", categoria);
        model.addAttribute("precioMin", precioMin);
        model.addAttribute("precioMax", precioMax);
        model.addAttribute("soloDisponibles", soloDisponibles);
        return "catalogo/catalogo";
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

    //CB-05 Ofertas
    @GetMapping("/catalogo/ofertas")
    public String ofertas(Model model) {
        List<Producto> resultados = productoService.listarProductosActivos().stream()
                .filter(p -> p.getPrecioOferta() != null)
                .collect(Collectors.toList());
        model.addAttribute("resultados", resultados);
        model.addAttribute("totalResultados", resultados.size());
        return "catalogo/ofertas";
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
