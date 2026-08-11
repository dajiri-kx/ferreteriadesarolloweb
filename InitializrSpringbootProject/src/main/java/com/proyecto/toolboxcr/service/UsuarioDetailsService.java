package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userDetailsService")
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final HttpSession session;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository, HttpSession session) {
        this.usuarioRepository = usuarioRepository;
        this.session = session;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        // En este proyecto, el username de login es el correo electrónico
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if ("bloqueado".equals(usuario.getEstado())) {
            throw new UsernameNotFoundException("Usuario bloqueado: " + username);
        }
        if ("inactivo".equals(usuario.getEstado())) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        // Guardamos el usuario en la sesión para compatibilidad con las vistas existentes
        session.setAttribute("usuarioLogueado", usuario);

        // Mapeamos el rol actual del usuario a un GrantedAuthority de Spring Security (ej: ROLE_CLIENTE, ROLE_ADMINISTRADOR)
        String rolFormateado = "ROLE_" + usuario.getRol().toUpperCase();
        var authorities = Collections.singleton(new SimpleGrantedAuthority(rolFormateado));

        return new User(usuario.getCorreo(), usuario.getContrasenaHash(), authorities);
    }
}
