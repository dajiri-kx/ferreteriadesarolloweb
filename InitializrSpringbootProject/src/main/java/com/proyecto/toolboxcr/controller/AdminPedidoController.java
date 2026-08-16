package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.DetallePedido;
import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.service.PedidoService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/pedidos")
public class AdminPedidoController {

    @Autowired
    private PedidoService pedidoService;

    /* A-03 — Listado de pedidos con filtros por estado y fecha */
    @GetMapping
    public String listado(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model model) {

        model.addAttribute("pedidos", pedidoService.getPedidosAdministracion(estado, fechaInicio, fechaFin));
        model.addAttribute("estados", pedidoService.getEstadosAdministracion());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        return "admin/pedidos/listado";
    }

    /* A-03 — Detalle administrativo de un pedido */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
        try {
            Pedido pedido = pedidoService.getPedidoAdministracion(id);
            List<DetallePedido> detalles = pedidoService.getDetalles(pedido);

            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", detalles);
            model.addAttribute("estados", pedidoService.getEstadosAdministracion());

            return "admin/pedidos/detalle";
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/pedidos";
        }
    }

    /* A-03 — Cambio de estado del pedido */
    @PostMapping("/{id}/estado")
    public String actualizarEstado(
            @PathVariable Long id,
            @RequestParam String estado,
            RedirectAttributes redirectAttrs) {

        try {
            pedidoService.actualizarEstadoAdministracion(id, estado);
            redirectAttrs.addFlashAttribute("todoOk", "El estado del pedido fue actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/pedidos/" + id;
    }
}
