
package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogoController {

    private final ProductoService productoService;

    public CatalogoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
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
}