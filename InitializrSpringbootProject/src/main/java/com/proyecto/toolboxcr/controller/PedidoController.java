package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.DetallePedido;
import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    private Usuario getUsuario(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    /* CU-04 — Historial */
    @GetMapping
    public String historial(HttpSession session, Model model) {
        model.addAttribute("pedidos", pedidoService.getHistorial(getUsuario(session)));
        return "pedidos/historial";
    }

    /* CU-04 — Detalle de pedido */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, HttpSession session,
                          Model model, RedirectAttributes redirectAttrs) {
        try {
            Pedido pedido = pedidoService.getDetalle(id, getUsuario(session));
            List<DetallePedido> detalles = pedidoService.getDetalles(pedido);
            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", detalles);
            return "pedidos/detalle";
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/perfil/pedidos";
        }
    }
}
