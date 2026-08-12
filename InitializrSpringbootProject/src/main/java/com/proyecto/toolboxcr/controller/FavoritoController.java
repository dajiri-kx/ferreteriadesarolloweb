package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Favorito;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.domain.Carrito;
import com.proyecto.toolboxcr.service.FavoritoService;
import com.proyecto.toolboxcr.service.CarritoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil/favoritos")
public class FavoritoController {

    @Autowired
    private FavoritoService favoritoService;

    @Autowired
    private CarritoService carritoService;

    private Usuario getUsuario(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    private void actualizarBadge(HttpSession session, Usuario usuario) {
        Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
        session.setAttribute("carritoCount", carritoService.contarItems(carrito));
    }

    @GetMapping
    public String listado(HttpSession session, Model model) {
        Usuario usuario = getUsuario(session);
        List<Favorito> favoritos = favoritoService.listar(usuario);
        model.addAttribute("favoritos", favoritos);
        return "favoritos/listado";
    }

    @PostMapping("/toggle")
    public String toggle(@RequestParam Long productoId,
                         HttpSession session,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            boolean added = favoritoService.toggle(usuario, productoId);
            
            // Actualizar la lista en la sesión
            Set<Long> ids = favoritoService.obtenerFavoritosIdsPorCliente(usuario);
            session.setAttribute("favoritosIds", ids);

            if (added) {
                redirectAttrs.addFlashAttribute("todoOk", "Producto agregado a favoritos.");
            } else {
                redirectAttrs.addFlashAttribute("todoOk", "Producto eliminado de favoritos.");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/perfil/favoritos");
    }

    @PostMapping("/mover")
    public String moverAlCarrito(@RequestParam Long productoId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            favoritoService.moverAlCarrito(usuario, productoId);

            // Actualizar favoritos en sesión
            Set<Long> ids = favoritoService.obtenerFavoritosIdsPorCliente(usuario);
            session.setAttribute("favoritosIds", ids);

            // Actualizar badge del carrito
            actualizarBadge(session, usuario);

            redirectAttrs.addFlashAttribute("todoOk", "Producto movido al carrito.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/perfil/favoritos";
    }
}
