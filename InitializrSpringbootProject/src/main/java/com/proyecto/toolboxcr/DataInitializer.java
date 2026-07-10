package com.proyecto.toolboxcr;

import com.proyecto.toolboxcr.domain.Categoria;
import com.proyecto.toolboxcr.repositorio.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initCategorias(CategoriaRepository categoriaRepository) {
        return args -> {
            if (categoriaRepository.count() == 0) {
                Categoria c1 = new Categoria();
                c1.setNombre("Herramientas");
                categoriaRepository.save(c1);

                Categoria c2 = new Categoria();
                c2.setNombre("Tornillería");
                categoriaRepository.save(c2);

                Categoria c3 = new Categoria();
                c3.setNombre("Pinturas");
                categoriaRepository.save(c3);

                Categoria c4 = new Categoria();
                c4.setNombre("Eléctrico");
                categoriaRepository.save(c4);

                Categoria c5 = new Categoria();
                c5.setNombre("Plomería");
                categoriaRepository.save(c5);
            }
        };
    }
}