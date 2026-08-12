package com.proyecto.toolboxcr.config;

import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final UsuarioRepository usuarioRepository;

    public CustomAuthenticationFailureHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // En nuestro login de Spring Security, el input field es 'correo'
        String email = request.getParameter("correo");
        if (email == null || email.trim().isEmpty()) {
            email = request.getParameter("username");
        }

        if (email != null && !email.trim().isEmpty()) {
            Usuario usuario = usuarioRepository.findByCorreo(email).orElse(null);
            if (usuario != null && !"bloqueado".equals(usuario.getEstado())) {
                usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
                usuarioRepository.save(usuario);
            }
        }

        // Redireccionar de vuelta al login con error
        response.sendRedirect(request.getContextPath() + "/login?error=true");
    }
}
