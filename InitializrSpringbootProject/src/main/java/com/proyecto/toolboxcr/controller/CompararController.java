package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.service.ProductoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/catalogo/comparar")
public class CompararController {

    private final ProductoService productoService;

    public CompararController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @SuppressWarnings("unchecked")
    private List<Long> getIds(HttpSession session) {
        List<Long> ids = (List<Long>) session.getAttribute("compararIds");
        if (ids == null) {
            ids = new ArrayList<>();
            session.setAttribute("compararIds", ids);
        }
        return ids;
    }

    @PostMapping("/agregar")
    public String agregar(@RequestParam Long id,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttrs) {
        List<Long> ids = getIds(session);
        if (ids.size() >= 4) {
            redirectAttrs.addFlashAttribute("error",
                    "Solo puedes comparar hasta 4 productos a la vez.");
        } else if (!ids.contains(id)) {
            ids.add(id);
            session.setAttribute("compararIds", ids);
            session.setAttribute("compararCount", ids.size());
            redirectAttrs.addFlashAttribute("todoOk", "Producto agregado a la comparación.");
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/catalogo");
    }

    @GetMapping("/quitar")
    public String quitar(@RequestParam Long id,
            HttpSession session,
            RedirectAttributes redirectAttrs) {
        List<Long> ids = getIds(session);
        ids.remove(id);
        session.setAttribute("compararIds", ids);
        session.setAttribute("compararCount", ids.size());
        return "redirect:/catalogo/comparar";
    }

    @GetMapping("/limpiar")
    public String limpiar(HttpSession session) {
        session.removeAttribute("compararIds");
        session.setAttribute("compararCount", 0);
        return "redirect:/catalogo";
    }

    @GetMapping
    public String comparar(HttpSession session, Model model) {
        List<Long> ids = getIds(session);
        List<Producto> productos = new ArrayList<>();
        for (Long id : ids) {
            productoService.obtenerProducto(id).ifPresent(productos::add);
        }
        model.addAttribute("productos", productos);
        return "catalogo/comparar";
    }
}
