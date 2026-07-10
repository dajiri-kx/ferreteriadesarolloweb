package com.proyecto.toolboxcr.service;

import com.opencsv.CSVReader;
import com.proyecto.toolboxcr.domain.Categoria;
import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.domain.ProductoImagen;
import com.proyecto.toolboxcr.repositorio.CategoriaRepository;
import com.proyecto.toolboxcr.repositorio.ProductoImagenRepository;
import com.proyecto.toolboxcr.repositorio.ProductoRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoImagenRepository productoImagenRepository;
    private final CategoriaRepository categoriaRepository;
    private final FirebaseStorageService firebaseStorageService;

    public ProductoService(ProductoRepository productoRepository,
            ProductoImagenRepository productoImagenRepository,
            CategoriaRepository categoriaRepository,
            FirebaseStorageService firebaseStorageService) {
        this.productoRepository = productoRepository;
        this.productoImagenRepository = productoImagenRepository;
        this.categoriaRepository = categoriaRepository;
        this.firebaseStorageService = firebaseStorageService;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosActivos() {
        return productoRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Producto> obtenerProducto(Long id) {
        return productoRepository.findById(id);
    }

    @Transactional
    public void guardar(Producto producto) {
        productoRepository.save(producto);
    }

    // Guarda el producto y, si viene un archivo, lo sube y lo marca como imagen principal.
    @Transactional
    public void guardar(Producto producto, MultipartFile imagenFile) {
        productoRepository.save(producto);

        if (imagenFile != null && !imagenFile.isEmpty()) {
            try {
                String url = firebaseStorageService.uploadImage(imagenFile, "producto", producto.getId());
                if (url != null) {
                    ProductoImagen imagen = productoImagenRepository
                            .findByProductoAndEsPrincipalTrue(producto);
                    if (imagen == null) {
                        imagen = new ProductoImagen();
                        imagen.setProducto(producto);
                        imagen.setEsPrincipal(true);
                        imagen.setOrden(0);
                    }
                    imagen.setUrlImagen(url);
                    productoImagenRepository.save(imagen);
                }
            } catch (IOException e) {
                // No se pudo subir la imagen; el producto ya quedó guardado igual.
            }
        }
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Optional<Producto> producto = productoRepository.findById(id);
        if (producto.isPresent()) {
            Producto p = producto.get();
            p.setActivo(activo);
            productoRepository.save(p);
        }
    }

    // A-01: "el admin puede eliminar productos"
    @Transactional
    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new IllegalArgumentException("El producto con ID " + id + " no existe.");
        }
        try {
            productoRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar el producto, tiene datos asociados (pedidos, carrito, inventario).");
        }
    }

    // A-01, criterio de aceptación 2: "El sistema permite carga masiva de
    // productos desde archivo CSV."
    // Formato esperado del CSV (con encabezado, separado por comas):
    // sku,nombre,descripcion,precio,idCategoria,marca,material,dimensiones
    @Transactional
    public ResultadoCarga importarCsv(MultipartFile archivoCsv)
        throws IOException, com.opencsv.exceptions.CsvException {
        int exitosos = 0;
        List<String> errores = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(archivoCsv.getInputStream(), StandardCharsets.UTF_8))) {

            List<String[]> filas = reader.readAll();
            boolean esEncabezado = true;
            int numeroFila = 1;

            for (String[] fila : filas) {
                numeroFila++;
                if (esEncabezado) {
                    esEncabezado = false;
                    continue;
                }
                if (fila.length < 5) {
                    errores.add("Fila " + numeroFila + ": columnas insuficientes.");
                    continue;
                }
                try {
                    String sku = fila[0].trim();
                    String nombre = fila[1].trim();
                    String descripcion = fila[2].trim();
                    BigDecimal precio = new BigDecimal(fila[3].trim());
                    Long idCategoria = Long.parseLong(fila[4].trim());
                    String marca = fila.length > 5 ? fila[5].trim() : null;
                    String material = fila.length > 6 ? fila[6].trim() : null;
                    String dimensiones = fila.length > 7 ? fila[7].trim() : null;

                    if (productoRepository.existsBySkuIgnoreCase(sku)) {
                        errores.add("Fila " + numeroFila + ": el SKU '" + sku + "' ya existe, se omitió.");
                        continue;
                    }

                    Categoria categoria = categoriaRepository.findById(idCategoria)
                            .orElseThrow(() -> new IllegalArgumentException(
                            "categoría " + idCategoria + " no existe"));

                    Producto p = new Producto();
                    p.setSku(sku);
                    p.setNombre(nombre);
                    p.setDescripcion(descripcion);
                    p.setPrecio(precio);
                    p.setCategoria(categoria);
                    p.setMarca(marca);
                    p.setMaterial(material);
                    p.setDimensiones(dimensiones);
                    p.setActivo(true);
                    productoRepository.save(p);
                    exitosos++;
                } catch (Exception e) {
                    errores.add("Fila " + numeroFila + ": " + e.getMessage());
                }
            }
        }
        return new ResultadoCarga(exitosos, errores);
    }

    // Clase interna para devolver el resultado de la carga al controller
    public static class ResultadoCarga {

        public final int exitosos;
        public final List<String> errores;

        public ResultadoCarga(int exitosos, List<String> errores) {
            this.exitosos = exitosos;
            this.errores = errores;
        }
    }
}
