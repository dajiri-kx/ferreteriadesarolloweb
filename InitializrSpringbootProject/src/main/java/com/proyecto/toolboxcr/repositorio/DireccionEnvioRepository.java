package com.proyecto.toolboxcr.repositorio;

import com.proyecto.toolboxcr.domain.DireccionEnvio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DireccionEnvioRepository extends JpaRepository<DireccionEnvio, Long> {

    List<DireccionEnvio> findByClienteIdOrderByEsPredeterminadaDesc(Long clienteId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE DIRECCION_ENVIO SET es_predeterminada = 0 WHERE cliente_id = :cid", nativeQuery = true)
    void desmarcarTodas(@Param("cid") Long cid);

    @Modifying
    @Transactional
    @Query(value = "UPDATE DIRECCION_ENVIO SET es_predeterminada = 1 WHERE id = :id", nativeQuery = true)
    void marcarPredeterminada(@Param("id") Long id);
}
