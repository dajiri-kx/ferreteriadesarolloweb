package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.DireccionEnvio;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.DireccionEnvioRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DireccionService {

    @Autowired
    private DireccionEnvioRepository direccionRepo;

    /* CU-03 — Listar direcciones del cliente */
    public List<DireccionEnvio> listar(Usuario cliente) {
        return direccionRepo.findByClienteIdOrderByEsPredeterminadaDesc(cliente.getId());
    }

    /* CU-03 — Guardar (crear o actualizar) */
    public void guardar(Long id, String alias, String direccion,
                        String codigoPostal, Boolean esPredeterminada, Usuario cliente) {
        if (codigoPostal == null || !codigoPostal.matches("\\d{4,10}")) {
            throw new IllegalArgumentException("Formato de código postal inválido (4–10 dígitos).");
        }

        DireccionEnvio dir;
        if (id != null) {
            // Actualización — verificar pertenencia
            dir = direccionRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada."));
            if (!dir.getCliente().getId().equals(cliente.getId())) {
                throw new IllegalArgumentException("No autorizado.");
            }
        } else {
            dir = new DireccionEnvio();
            dir.setCliente(cliente);
        }

        dir.setAlias(alias);
        dir.setDireccion(direccion);
        dir.setCodigoPostal(codigoPostal);

        if (Boolean.TRUE.equals(esPredeterminada)) {
            direccionRepo.desmarcarTodas(cliente.getId());
            dir.setEsPredeterminada(true);
        } else {
            dir.setEsPredeterminada(false);
        }
        direccionRepo.save(dir);
    }

    /* CU-03 — Eliminar */
    public void eliminar(Long id, Usuario cliente) {
        DireccionEnvio dir = direccionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada."));
        if (!dir.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("No autorizado.");
        }
        direccionRepo.delete(dir);
    }

    /* CU-03 — Marcar como predeterminada */
    public void setPredeterminada(Long id, Usuario cliente) {
        direccionRepo.desmarcarTodas(cliente.getId());
        direccionRepo.marcarPredeterminada(id);
    }
}
