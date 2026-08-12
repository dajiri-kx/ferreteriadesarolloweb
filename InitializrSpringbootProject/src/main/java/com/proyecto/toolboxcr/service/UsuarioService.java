package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /* CU-01 — Registrar cuenta */
    public void registrar(String nombre, String correo, String contrasena, String telefono) {
        if (usuarioRepo.existsByCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }
        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        nuevo.setContrasenaHash(passwordEncoder.encode(contrasena));
        nuevo.setTelefono(telefono);
        nuevo.setRol("cliente");
        nuevo.setEstado("activo");
        nuevo.setIntentosFallidos(0);
        nuevo.setFechaRegistro(LocalDateTime.now());
        usuarioRepo.save(nuevo);
    }

    /* CU-02 — Iniciar sesión */
    public Usuario login(String correo, String contrasena) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Correo o contraseña incorrectos."));

        /* Desbloquear si pasaron los 15 minutos */
        if ("bloqueado".equals(usuario.getEstado()) && usuario.getFechaBloqueo() != null) {
            if (usuario.getFechaBloqueo().plusMinutes(15).isBefore(LocalDateTime.now())) {
                usuario.setEstado("activo");
                usuario.setIntentosFallidos(0);
                usuarioRepo.save(usuario);
            }
        }

        if ("bloqueado".equals(usuario.getEstado())) {
            throw new IllegalStateException("bloqueado");
        }

        if (!passwordEncoder.matches(contrasena, usuario.getContrasenaHash()) && !hashSHA256(contrasena).equals(usuario.getContrasenaHash())) {
            /* El trigger trg_usuario_bloqueo_intentos bloquea automáticamente al llegar a 5 */
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            usuarioRepo.save(usuario);
            throw new IllegalArgumentException("Correo o contraseña incorrectos.");
        }

        /* Login exitoso — el trigger resetea intentos_fallidos al cambiar ultima_sesion */
        usuario.setUltimaSesion(LocalDateTime.now());
        usuarioRepo.save(usuario);
        return usuarioRepo.findByCorreo(correo).orElse(usuario); // recarga para reflejar trigger
    }

    /* Utilidad — SHA-256 sin dependencias externas */
    private String hashSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al hashear la contraseña", e);
        }
    }

    public java.util.Optional<Usuario> getUsuarioPorUsername(String username) {
        return usuarioRepo.findByCorreo(username);
    }

    public java.util.List<String> getRolesNombres() {
        return java.util.Arrays.asList("cliente", "administrador", "bodega", "dueño");
    }

    public void asignarRolPorUsername(String username, String rolNombre) {
        Usuario usuario = usuarioRepo.findByCorreo(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        usuario.setRol(rolNombre.toLowerCase());
        usuarioRepo.save(usuario);
    }

    public void eliminarRol(String username, Integer idRol) {
        Usuario usuario = usuarioRepo.findByCorreo(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        usuario.setRol("cliente");
        usuarioRepo.save(usuario);
    }
}
