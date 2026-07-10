package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.Categoria;
import com.proyecto.toolboxcr.repositorio.CategoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }
}