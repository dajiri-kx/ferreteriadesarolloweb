package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.*;
import com.proyecto.toolboxcr.repositorio.CuponRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.proyecto.toolboxcr.repositorio.CategoriaRepository;
import com.proyecto.toolboxcr.repositorio.ProductoRepository;
import java.util.Comparator;

@Service
public class CuponService {

    @Autowired
    private CuponRepository cuponRepo;
    @Autowired
    private ProductoRepository productoRepo;
    @Autowired
    private CategoriaRepository categoriaRepo;

    public Cupon validarCupon(String codigo, List<ItemCarrito> items) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de cupón no puede estar vacío.");
        }

        Cupon cupon = cuponRepo.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new IllegalArgumentException("El código de cupón no es válido."));

        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(cupon.getFechaInicio()) || hoy.isAfter(cupon.getFechaFin())) {
            throw new IllegalArgumentException("El cupón ha expirado o aún no está activo.");
        }

        if (cupon.getUsosActuales() >= cupon.getLimiteUsos()) {
            throw new IllegalArgumentException("El cupón ha alcanzado su límite de usos.");
        }

        // Validar si aplica a algún artículo en el carrito
        if ("producto".equals(cupon.getAplicaA())) {
            boolean tieneProducto = items.stream()
                    .anyMatch(item -> item.getProducto().getId().equals(cupon.getProducto().getId()));
            if (!tieneProducto) {
                throw new IllegalArgumentException("Este cupón solo aplica para el producto: " + cupon.getProducto().getNombre());
            }
        } else if ("categoria".equals(cupon.getAplicaA())) {
            boolean tieneCategoria = items.stream()
                    .anyMatch(item -> item.getProducto().getCategoria() != null
                    && item.getProducto().getCategoria().getId().equals(cupon.getCategoria().getId()));
            if (!tieneCategoria) {
                throw new IllegalArgumentException("Este cupón solo aplica para productos de la categoría: " + cupon.getCategoria().getNombre());
            }
        }

        return cupon;
    }

    public BigDecimal calcularDescuento(Cupon cupon, List<ItemCarrito> items) {
        if (cupon == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal descuento = BigDecimal.ZERO;
        if ("carrito".equals(cupon.getAplicaA())) {
            BigDecimal subtotal = items.stream()
                    .map(ItemCarrito::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if ("porcentaje".equals(cupon.getTipo())) {
                descuento = subtotal.multiply(cupon.getValor()).divide(new BigDecimal("100.00"));
            } else { // monto_fijo
                descuento = cupon.getValor();
            }
        } else if ("producto".equals(cupon.getAplicaA())) {
            for (ItemCarrito item : items) {
                if (item.getProducto().getId().equals(cupon.getProducto().getId())) {
                    BigDecimal subtotalProd = item.getSubtotal();
                    if ("porcentaje".equals(cupon.getTipo())) {
                        descuento = descuento.add(subtotalProd.multiply(cupon.getValor()).divide(new BigDecimal("100.00")));
                    } else { // monto_fijo
                        descuento = descuento.add(cupon.getValor());
                    }
                }
            }
        } else if ("categoria".equals(cupon.getAplicaA())) {
            for (ItemCarrito item : items) {
                if (item.getProducto().getCategoria() != null
                        && item.getProducto().getCategoria().getId().equals(cupon.getCategoria().getId())) {
                    BigDecimal subtotalProd = item.getSubtotal();
                    if ("porcentaje".equals(cupon.getTipo())) {
                        descuento = descuento.add(subtotalProd.multiply(cupon.getValor()).divide(new BigDecimal("100.00")));
                    } else { // monto_fijo
                        descuento = descuento.add(cupon.getValor());
                    }
                }
            }
        }

        // El descuento total no puede superar el subtotal de los productos aplicables
        BigDecimal subtotalMax = items.stream()
                .map(ItemCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (descuento.compareTo(subtotalMax) > 0) {
            descuento = subtotalMax;
        }

        return descuento;
    }

    /* A-04 — Listado administrativo de cupones */
    public List<Cupon> getCuponesAdministracion() {
        return cuponRepo.findAll()
                .stream()
                .sorted(Comparator.comparing(Cupon::getFechaInicio).reversed())
                .toList();
    }

    /* A-04 — Productos disponibles para aplicar descuentos */
    public List<Producto> getProductosParaCupones() {
        return productoRepo.findByActivoTrue();
    }

    /* A-04 — Categorías disponibles para aplicar descuentos */
    public List<Categoria> getCategoriasParaCupones() {
        return categoriaRepo.findAllByOrderByNombreAsc();
    }

    /* A-04 — Tipos de descuento permitidos */
    public List<String> getTiposCupon() {
        return List.of("porcentaje", "monto_fijo");
    }

    /* A-04 — Ámbitos de aplicación permitidos */
    public List<String> getAplicacionesCupon() {
        return List.of("producto", "categoria", "carrito");
    }

    /* A-04 — Creación administrativa de cupón/descuento */
    @org.springframework.transaction.annotation.Transactional
    public Cupon crearCuponAdministracion(
            String codigo,
            String tipo,
            BigDecimal valor,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer limiteUsos,
            String aplicaA,
            Long productoId,
            Long categoriaId) {

        validarDatosCupon(codigo, tipo, valor, fechaInicio, fechaFin, limiteUsos, aplicaA, productoId, categoriaId);

        Cupon cupon = new Cupon();
        cupon.setCodigo(codigo.trim().toUpperCase());
        cupon.setTipo(tipo);
        cupon.setValor(valor);
        cupon.setFechaInicio(fechaInicio);
        cupon.setFechaFin(fechaFin);
        cupon.setLimiteUsos(limiteUsos);
        cupon.setUsosActuales(0);
        cupon.setAplicaA(aplicaA);

        if ("producto".equals(aplicaA)) {
            Producto producto = productoRepo.findById(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
            cupon.setProducto(producto);
        }

        if ("categoria".equals(aplicaA)) {
            Categoria categoria = categoriaRepo.findById(categoriaId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));
            cupon.setCategoria(categoria);
        }

        return cuponRepo.save(cupon);
    }

    private void validarDatosCupon(
            String codigo,
            String tipo,
            BigDecimal valor,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer limiteUsos,
            String aplicaA,
            Long productoId,
            Long categoriaId) {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del cupón es obligatorio.");
        }

        if (!getTiposCupon().contains(tipo)) {
            throw new IllegalArgumentException("El tipo de descuento no es válido.");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del descuento debe ser mayor a cero.");
        }

        if ("porcentaje".equals(tipo) && valor.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje no puede ser mayor a 100.");
        }

        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas del cupón son obligatorias.");
        }

        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        if (limiteUsos == null || limiteUsos <= 0) {
            throw new IllegalArgumentException("El límite de usos debe ser mayor a cero.");
        }

        if (!getAplicacionesCupon().contains(aplicaA)) {
            throw new IllegalArgumentException("La aplicación del cupón no es válida.");
        }

        if ("producto".equals(aplicaA) && productoId == null) {
            throw new IllegalArgumentException("Debe seleccionar un producto para este cupón.");
        }

        if ("categoria".equals(aplicaA) && categoriaId == null) {
            throw new IllegalArgumentException("Debe seleccionar una categoría para este cupón.");
        }

        if (cuponRepo.findByCodigoIgnoreCase(codigo.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cupón con ese código.");
        }
    }
}
