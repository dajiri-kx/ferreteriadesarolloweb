package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Categoria;
import com.proyecto.toolboxcr.domain.Cupon;
import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.service.CuponService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/cupones")
public class AdminCuponController {

    @Autowired
    private CuponService cuponService;

    /* A-04 — Listado y reporte básico de cupones */
    @GetMapping
    public String listado(Model model) {
        model.addAttribute("cupones", cuponService.getCuponesAdministracion());
        model.addAttribute("productos", cuponService.getProductosParaCupones());
        model.addAttribute("categorias", cuponService.getCategoriasParaCupones());
        model.addAttribute("tipos", cuponService.getTiposCupon());
        model.addAttribute("aplicaciones", cuponService.getAplicacionesCupon());

        return "admin/cupones/listado";
    }

    /* A-04 — Crear descuento/cupón por producto, categoría o carrito */
    @PostMapping("/guardar")
    public String guardar(
            @RequestParam String codigo,
            @RequestParam String tipo,
            @RequestParam java.math.BigDecimal valor,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam Integer limiteUsos,
            @RequestParam String aplicaA,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long categoriaId,
            RedirectAttributes redirectAttrs) {

        try {
            cuponService.crearCuponAdministracion(
                    codigo,
                    tipo,
                    valor,
                    fechaInicio,
                    fechaFin,
                    limiteUsos,
                    aplicaA,
                    productoId,
                    categoriaId
            );
            redirectAttrs.addFlashAttribute("todoOk", "El cupón fue creado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/cupones";
    }
}
