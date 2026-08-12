package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.*;
import com.proyecto.toolboxcr.repositorio.CuponRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CuponService {

    @Autowired private CuponRepository cuponRepo;

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
                    .anyMatch(item -> item.getProducto().getCategoria() != null &&
                            item.getProducto().getCategoria().getId().equals(cupon.getCategoria().getId()));
            if (!tieneCategoria) {
                throw new IllegalArgumentException("Este cupón solo aplica para productos de la categoría: " + cupon.getCategoria().getNombre());
            }
        }

        return cupon;
    }

    public BigDecimal calcularDescuento(Cupon cupon, List<ItemCarrito> items) {
        if (cupon == null) return BigDecimal.ZERO;

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
                if (item.getProducto().getCategoria() != null &&
                        item.getProducto().getCategoria().getId().equals(cupon.getCategoria().getId())) {
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
}
