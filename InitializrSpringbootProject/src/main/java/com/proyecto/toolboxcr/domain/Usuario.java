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
}
