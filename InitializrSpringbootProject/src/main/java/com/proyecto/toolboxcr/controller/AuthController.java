package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    /* CU-02 — Mostrar login */
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login/login";
    }

    /* CU-02 — Procesar login */
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String contrasena,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = usuarioService.login(correo, contrasena);
            session.setAttribute("usuarioLogueado", usuario);
            return "redirect:/";
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error",
                    "Su cuenta está bloqueada. Intente nuevamente en 15 minutos.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    /* CU-02 — Cerrar sesión */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /* CU-01 — Mostrar formulario de registro */
    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "usuarios/registro";
    }

    /* CU-01 — Procesar registro */
    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String nombre,
                                   @RequestParam String correo,
                                   @RequestParam String contrasena,
                                   @RequestParam(required = false) String telefono,
                                   RedirectAttributes redirectAttrs) {
        try {
            usuarioService.registrar(nombre, correo, contrasena, telefono);
            redirectAttrs.addFlashAttribute("todoOk",
                    "¡Cuenta creada exitosamente! Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro";
        }
    }
}
