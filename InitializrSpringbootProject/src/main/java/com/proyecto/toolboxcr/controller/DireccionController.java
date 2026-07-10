package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.DireccionEnvio;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.service.DireccionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil/direcciones")
public class DireccionController {

    @Autowired
    private DireccionService direccionService;

    private Usuario getUsuario(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    /* CU-03 — Listado */
    @GetMapping
    public String listado(HttpSession session, Model model) {
        model.addAttribute("direcciones", direccionService.listar(getUsuario(session)));
        return "direcciones/listado";
    }

    /* CU-03 — Guardar (crear o editar) */
    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long id,
                          @RequestParam(required = false) String alias,
                          @RequestParam String direccion,
                          @RequestParam String codigoPostal,
                          @RequestParam(required = false, defaultValue = "false") Boolean esPredeterminada,
                          HttpSession session, RedirectAttributes redirectAttrs) {
        try {
            direccionService.guardar(id, alias, direccion, codigoPostal,
                    esPredeterminada, getUsuario(session));
            redirectAttrs.addFlashAttribute("todoOk", "Dirección guardada correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/perfil/direcciones";
    }

    /* CU-03 — Marcar predeterminada */
    @PostMapping("/predeterminada")
    public String setPredeterminada(@RequestParam Long id,
                                    HttpSession session, RedirectAttributes redirectAttrs) {
        direccionService.setPredeterminada(id, getUsuario(session));
        redirectAttrs.addFlashAttribute("todoOk", "Dirección predeterminada actualizada.");
        return "redirect:/perfil/direcciones";
    }

    /* CU-03 — Eliminar */
    @PostMapping("/eliminar")
    public String eliminar(@RequestParam Long id,
                           HttpSession session, RedirectAttributes redirectAttrs) {
        try {
            direccionService.eliminar(id, getUsuario(session));
            redirectAttrs.addFlashAttribute("todoOk", "Dirección eliminada.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/perfil/direcciones";
    }
}
