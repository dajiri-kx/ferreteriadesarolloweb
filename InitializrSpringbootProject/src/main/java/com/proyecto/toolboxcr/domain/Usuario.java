package com.proyecto.toolboxcr.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "USUARIO")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private String username;

    public String getUsername() {
        return this.correo;
    }

    public void setUsername(String username) {
        this.username = username;
        this.correo = username;
    }

    @NotNull
    @Size(max = 120)
    private String nombre;

    @NotNull
    @Size(max = 150)
    private String correo;

    @Column(name = "contrase\u00f1a_hash")
    private String contrasenaHash;

    @Size(max = 20)
    private String telefono;

    private String rol;

    private String estado;

    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "ultima_sesion")
    private LocalDateTime ultimaSesion;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public java.util.Set<Rol> getRoles() {
        if (this.rol == null) {
            return java.util.Collections.emptySet();
        }
        Rol r = new Rol();
        r.setRol(this.rol.toUpperCase());
        if ("administrador".equalsIgnoreCase(this.rol)) r.setIdRol(1);
        else if ("bodega".equalsIgnoreCase(this.rol)) r.setIdRol(2);
        else if ("dueño".equalsIgnoreCase(this.rol)) r.setIdRol(4);
        else r.setIdRol(3); // cliente / USER
        return java.util.Collections.singleton(r);
    }

    @lombok.Data
    public static class Rol {
        private Integer idRol;
        private String rol;
    }
}
