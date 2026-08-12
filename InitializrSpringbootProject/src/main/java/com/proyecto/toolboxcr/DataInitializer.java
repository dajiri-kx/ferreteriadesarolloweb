package com.proyecto.toolboxcr;

import com.proyecto.toolboxcr.domain.Categoria;
import com.proyecto.toolboxcr.repositorio.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Bean
    CommandLineRunner alterTable(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE PRODUCTO_IMAGEN MODIFY url_imagen VARCHAR(1024) NOT NULL");
                System.out.println(">>> ALTER TABLE PRODUCTO_IMAGEN ejecutado exitosamente (1024 caracteres)!");
            } catch (Exception e) {
                System.out.println(">>> Error al alterar tabla: " + e.getMessage());
            }
        };
    }
}