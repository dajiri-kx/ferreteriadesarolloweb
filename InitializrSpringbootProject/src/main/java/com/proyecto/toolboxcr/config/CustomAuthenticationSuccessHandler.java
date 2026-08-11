package com.proyecto.toolboxcr.config;

import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;

    public CustomAuthenticationSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByCorreo(email).orElse(null);

        if (usuario != null) {
            // Actualizar la última sesión
            usuario.setUltimaSesion(LocalDateTime.now());
            // Guardar para activar el reset de intentos en la DB
            usuarioRepository.save(usuario);

            // Cargar en la sesión HTTP
            HttpSession session = request.getSession();
            session.setAttribute("usuarioLogueado", usuario);
        }

        // Redireccionar al home
        response.sendRedirect(request.getContextPath() + "/");
    }
}
