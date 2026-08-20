package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.service.UsuarioService;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/usuario_rol")
public class UsuarioRolController {

    @Autowired
    private UsuarioService usuarioService;

    // 1. Endpoint para la vista inicial (sin usuario)
    @GetMapping("/mantenimiento")
    public String mantenimiento(Model model) {
        model.addAttribute("usuario", new Usuario());
        // Se inicializan listas vacías para evitar errores de Thymeleaf
        model.addAttribute("rolesAsignados", Collections.emptySet());
        model.addAttribute("rolesDisponibles", Collections.emptyList());
        return "usuario_rol/mantenimiento";
    }

    // 2. Endpoint para buscar y mostrar roles
    @GetMapping("/buscar")
    public String buscarUsuario(@RequestParam(value = "username", required = false) String username, Model model) {
        if (username == null || username.isBlank() || !username.contains("@") || !username.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            model.addAttribute("usuario", new Usuario());
            model.addAttribute("rolesAsignados", Collections.emptySet());
            model.addAttribute("rolesDisponibles", Collections.emptyList());
            model.addAttribute("error", "Debe ingresar un correo electrónico válido (ejemplo: usuario@dominio.com).");
            return "usuario_rol/mantenimiento";
        }

        try {
            Usuario usuario = usuarioService.getUsuarioPorUsername(username.trim()).orElse(null);
            model.addAttribute("usuario", usuario);

            if (usuario != null) {
                List<String> todosRolesNombres = usuarioService.getRolesNombres();
                List<String> rolesDisponibles = todosRolesNombres.stream()
                    .filter(rolNombre -> usuario.getRoles().stream()
                            .noneMatch(rolAsignado -> rolAsignado.getRol().equalsIgnoreCase(rolNombre)))
                    .toList();

                model.addAttribute("rolesAsignados", usuario.getRoles());
                model.addAttribute("rolesDisponibles", rolesDisponibles);
            } else {
                model.addAttribute("rolesAsignados", Collections.emptySet());
                model.addAttribute("rolesDisponibles", Collections.emptyList());
                model.addAttribute("error", "No se encontró ningún usuario con el correo " + username);
            }
        } catch (Exception e) {
            model.addAttribute("usuario", new Usuario());
            model.addAttribute("rolesAsignados", Collections.emptySet());
            model.addAttribute("rolesDisponibles", Collections.emptyList());
            model.addAttribute("error", "Error al procesar la búsqueda.");
        }

        return "usuario_rol/mantenimiento";
    }

    // 3. Endpoint para agregar un rol (o cambiar el rol actual)
    @GetMapping("/agregar")
    public String agregarRol(@RequestParam("username") String username, 
                             @RequestParam("nombreRol") String nombreRol) {
        
        usuarioService.asignarRolPorUsername(username, nombreRol);
        
        // Redirige al /buscar para recargar los datos del usuario actualizado
        return "redirect:/usuario_rol/buscar?username=" + username; 
    }

    // 4. Endpoint para eliminar un rol (revertir a cliente)
    @GetMapping("/eliminar")
    public String eliminarRol(@RequestParam("username") String username, 
                              @RequestParam("idRol") Integer idRol) {
        
        usuarioService.eliminarRol(username, idRol);
        
        // Redirige al /buscar para recargar los datos del usuario actualizado
        return "redirect:/usuario_rol/buscar?username=" + username;
    }
}
