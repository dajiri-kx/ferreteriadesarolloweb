package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Carrito;
import com.proyecto.toolboxcr.domain.ItemCarrito;
import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.MetodoEnvioRepository;
import com.proyecto.toolboxcr.service.CarritoService;
import com.proyecto.toolboxcr.service.DireccionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired private CarritoService carritoService;
    @Autowired private MetodoEnvioRepository metodoEnvioRepo;
    @Autowired private DireccionService direccionService;

    private Usuario getUsuario(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    private void actualizarBadge(HttpSession session, Usuario usuario) {
        Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
        session.setAttribute("carritoCount", carritoService.contarItems(carrito));
    }

    /* CC-01 — Ver carrito */
    @GetMapping
    public String verCarrito(HttpSession session, Model model) {
        Usuario usuario = getUsuario(session);
        Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
        List<ItemCarrito> items = carritoService.obtenerItems(carrito);
        BigDecimal subtotal = carritoService.calcularSubtotal(items);
        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        actualizarBadge(session, usuario);
        return "carrito/carrito";
    }

    /* CC-01 — Agregar producto al carrito */
    @PostMapping("/agregar")
    public String agregar(@RequestParam Long productoId,
                          @RequestParam(defaultValue = "1") int cantidad,
                          HttpSession session,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttrs) {
        try {
            carritoService.agregarItem(getUsuario(session), productoId, cantidad);
            actualizarBadge(session, getUsuario(session));
            redirectAttrs.addFlashAttribute("todoOk", "Producto agregado al carrito.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    /* CC-01 — Actualizar cantidad de un ítem */
    @PostMapping("/actualizar")
    public String actualizar(@RequestParam Long itemId,
                             @RequestParam int cantidad,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        try {
            carritoService.actualizarCantidad(getUsuario(session), itemId, cantidad);
            actualizarBadge(session, getUsuario(session));
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    /* CC-01 — Eliminar ítem */
    @PostMapping("/eliminar")
    public String eliminar(@RequestParam Long itemId,
                           HttpSession session,
                           RedirectAttributes redirectAttrs) {
        try {
            carritoService.eliminarItem(getUsuario(session), itemId);
            actualizarBadge(session, getUsuario(session));
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    /* CC-03 — Mostrar checkout */
    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model) {
        Usuario usuario = getUsuario(session);
        Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
        List<ItemCarrito> items = carritoService.obtenerItems(carrito);

        if (items.isEmpty()) {
            return "redirect:/carrito";
        }

        BigDecimal subtotal = carritoService.calcularSubtotal(items);
        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("metodos", metodoEnvioRepo.findAll());
        model.addAttribute("direcciones", direccionService.listar(usuario));
        return "carrito/checkout";
    }

    /* CC-03 — Confirmar pedido */
    @PostMapping("/confirmar")
    public String confirmar(@RequestParam Long metodoEnvioId,
                            @RequestParam(required = false) Long direccionId,
                            HttpSession session,
                            RedirectAttributes redirectAttrs) {
        try {
            Pedido pedido = carritoService.crearPedido(getUsuario(session), metodoEnvioId, direccionId);
            session.setAttribute("carritoCount", 0);
            redirectAttrs.addFlashAttribute("todoOk",
                    "¡Pedido " + pedido.getNumeroOrden() + " creado con éxito!");
            return "redirect:/perfil/pedidos/" + pedido.getId();
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito/checkout";
        }
    }
}
