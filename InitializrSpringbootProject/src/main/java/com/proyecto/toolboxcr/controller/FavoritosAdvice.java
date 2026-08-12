package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.service.FavoritoService;
import jakarta.servlet.http.HttpSession;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class FavoritosAdvice {

    @Autowired
    private FavoritoService favoritoService;

    @Autowired
    private HttpSession session;

    @ModelAttribute
    public void populateFavoritos() {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario != null) {
            if (session.getAttribute("favoritosIds") == null) {
                Set<Long> ids = favoritoService.obtenerFavoritosIdsPorCliente(usuario);
                session.setAttribute("favoritosIds", ids);
            }
        } else {
            session.removeAttribute("favoritosIds");
        }
    }
}
