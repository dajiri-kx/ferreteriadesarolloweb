package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.*;
import com.proyecto.toolboxcr.repositorio.PagoRepository;
import com.proyecto.toolboxcr.repositorio.PedidoRepository;
import com.proyecto.toolboxcr.repositorio.InventarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoService {

    @Autowired private PagoRepository pagoRepo;
    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private CarritoService carritoService;
    @Autowired private InventarioRepository inventarioRepo;
    @PersistenceContext private EntityManager entityManager;

    @Transactional
    public Pago procesarPagoSimulado(Pedido pedido, String metodo, boolean exito, String motivoFallo) {
        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMonto(pedido.getTotal());
        pago.setMetodo(metodo);
        pago.setFecha(LocalDateTime.now());

        if (exito) {
            pago.setEstado("completado");
            pago.setReferenciaGateway("ref_sim_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
            pagoRepo.save(pago);

            // Actualizar estado del pedido a 'preparando'
            pedido.setEstado("preparando");
            pedidoRepo.save(pedido);

            // Descontar stock e insertar alertas de inventario bajo en Java (porque el usuario no tiene permisos de TRIGGER en Aiven DB)
            for (DetallePedido dp : pedido.getDetalles()) {
                Producto producto = dp.getProducto();
                int cantidad = dp.getCantidad();
                
                Inventario inventario = inventarioRepo.findByProducto(producto)
                    .orElseThrow(() -> new IllegalStateException("No hay inventario configurado para el producto ID: " + producto.getId()));
                
                int stockAnterior = inventario.getStockDisponible();
                int nuevoStock = stockAnterior - cantidad;
                if (nuevoStock < 0) {
                    throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
                }
                
                inventario.setStockDisponible(nuevoStock);
                inventarioRepo.save(inventario);
                
                // Si cruza o cae al umbral mínimo, y antes estaba por encima, generamos la alerta
                if (nuevoStock <= inventario.getUmbralMinimo() && stockAnterior > inventario.getUmbralMinimo()) {
                    entityManager.createNativeQuery(
                        "INSERT INTO ALERTA_INVENTARIO (producto_id, stock_al_momento, umbral_minimo, fecha_alerta, atendida) " +
                        "VALUES (:productoId, :stock, :umbral, NOW(), 0)"
                    )
                    .setParameter("productoId", producto.getId())
                    .setParameter("stock", nuevoStock)
                    .setParameter("umbral", inventario.getUmbralMinimo())
                    .executeUpdate();
                }
            }

            // Vaciar el carrito de compras
            Carrito carrito = carritoService.obtenerOCrearCarrito(pedido.getCliente());
            carritoService.limpiarCarrito(carrito);

            // Incrementar usos actuales del cupón
            if (pedido.getCupon() != null) {
                entityManager.createNativeQuery(
                    "UPDATE CUPON SET usos_actuales = usos_actuales + 1 WHERE id = :cuponId"
                )
                .setParameter("cuponId", pedido.getCupon().getId())
                .executeUpdate();
            }

            // Simular el envío de un correo de confirmación
            enviarCorreoConfirmacionSimulado(pedido, pago);
        } else {
            pago.setEstado("fallido");
            pago.setReferenciaGateway("ref_sim_failed_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            pagoRepo.save(pago);
            // El pedido sigue en estado 'pendiente' para reintentar
        }

        return pago;
    }

    private void enviarCorreoConfirmacionSimulado(Pedido pedido, Pago pago) {
        System.out.println("================================================================================");
        System.out.println("[EMAIL SIMULATION] enviando correo a: " + pedido.getCliente().getCorreo());
        System.out.println("Asunto: ¡Tu pago de la orden " + pedido.getNumeroOrden() + " ha sido confirmado!");
        System.out.println("Detalle de la Transacción:");
        System.out.println(" - Número de orden: " + pedido.getNumeroOrden());
        System.out.println(" - Referencia de Pago: " + pago.getReferenciaGateway());
        System.out.println(" - Método de Pago: " + pago.getMetodo().toUpperCase());
        System.out.println(" - Total pagado: ₡" + pedido.getTotal());
        System.out.println(" - Envío por método: " + pedido.getMetodoEnvio().getNombre());
        if (pedido.getDireccionEnvio() != null) {
            System.out.println(" - Dirección de envío: " + pedido.getDireccionEnvio().getDireccion());
        } else {
            System.out.println(" - Entrega: Retiro en tienda");
        }
        System.out.println("¡Gracias por comprar en ToolboxCR!");
        System.out.println("================================================================================");
    }
}
