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
    public String mostrarLogin(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               org.springframework.ui.Model model) {
        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos, o cuenta bloqueada.");
        }
        if (logout != null) {
            model.addAttribute("todoOk", "Ha cerrado sesión correctamente.");
        }
        return "login/login";
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
                                   @RequestParam(required = false) String confirmarContrasena,
                                   @RequestParam(required = false) String telefono,
                                   RedirectAttributes redirectAttrs) {
        if (confirmarContrasena != null && !contrasena.equals(confirmarContrasena)) {
            redirectAttrs.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/registro";
        }

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
