package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.*;
import com.proyecto.toolboxcr.repositorio.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarritoService {

    @Autowired private CarritoRepository carritoRepo;
    @Autowired private ItemCarritoRepository itemRepo;
    @Autowired private MetodoEnvioRepository metodoEnvioRepo;
    @Autowired private DireccionEnvioRepository direccionRepo;
    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private DetallePedidoRepository detalleRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private InventarioRepository inventarioRepo;

    /* CC-01 — Obtener o crear carrito del cliente */
    public Carrito obtenerOCrearCarrito(Usuario cliente) {
        return carritoRepo.findByCliente(cliente).orElseGet(() -> {
            Carrito c = new Carrito();
            c.setCliente(cliente);
            return carritoRepo.save(c);
        });
    }

    /* CC-01 — Agregar o acumular un producto en el carrito (upsert) */
    @Transactional
    public void agregarItem(Usuario cliente, Long productoId, int cantidad) {
        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        Inventario inv = inventarioRepo.findByProducto(producto)
                .orElseThrow(() -> new IllegalArgumentException("Sin información de stock."));

        Carrito carrito = obtenerOCrearCarrito(cliente);

        ItemCarrito item = itemRepo.findByCarritoAndProducto(carrito, producto)
                .orElseGet(() -> {
                    ItemCarrito nuevo = new ItemCarrito();
                    nuevo.setCarrito(carrito);
                    nuevo.setProducto(producto);
                    nuevo.setCantidad(0);
                    return nuevo;
                });

        int nuevaCantidad = item.getCantidad() + cantidad;
        if (inv.getStockDisponible() < nuevaCantidad) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Disponible: " + inv.getStockDisponible());
        }

        item.setCantidad(nuevaCantidad);
        itemRepo.save(item);
    }

    /* CC-01 — Actualizar cantidad de un ítem */
    @Transactional
    public void actualizarCantidad(Usuario cliente, Long itemId, int cantidad) {
        ItemCarrito item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado."));

        if (!item.getCarrito().getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("No autorizado.");
        }

        if (cantidad <= 0) {
            itemRepo.delete(item);
            return;
        }

        Inventario inv = inventarioRepo.findByProducto(item.getProducto())
                .orElseThrow(() -> new IllegalArgumentException("Sin información de stock."));

        if (inv.getStockDisponible() < cantidad) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Disponible: " + inv.getStockDisponible());
        }

        item.setCantidad(cantidad);
        itemRepo.save(item);
    }

    /* CC-01 — Eliminar un ítem del carrito */
    @Transactional
    public void eliminarItem(Usuario cliente, Long itemId) {
        ItemCarrito item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado."));

        if (!item.getCarrito().getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("No autorizado.");
        }

        itemRepo.delete(item);
    }

    /* CC-01 — Obtener todos los ítems del carrito */
    public List<ItemCarrito> obtenerItems(Carrito carrito) {
        return itemRepo.findByCarrito(carrito);
    }

    /* CC-01 — Calcular subtotal */
    public BigDecimal calcularSubtotal(List<ItemCarrito> items) {
        return items.stream()
                .map(ItemCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /* CC-01 — Contar unidades totales para el badge del navbar */
    public int contarItems(Carrito carrito) {
        return itemRepo.findByCarrito(carrito).stream()
                .mapToInt(ItemCarrito::getCantidad)
                .sum();
    }

    /* CC-03 — Vaciar ítems del carrito */
    @Transactional
    public void limpiarCarrito(Carrito carrito) {
        itemRepo.deleteByCarrito(carrito);
    }

    /* CC-03 — Crear pedido a partir del carrito y limpiar */
    @Transactional
    public Pedido crearPedido(Usuario cliente, Long metodoEnvioId, Long direccionId) {
        Carrito carrito = carritoRepo.findByCliente(cliente)
                .orElseThrow(() -> new IllegalArgumentException("Carrito vacío."));

        List<ItemCarrito> items = itemRepo.findByCarrito(carrito);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }

        MetodoEnvio metodo = metodoEnvioRepo.findById(metodoEnvioId)
                .orElseThrow(() -> new IllegalArgumentException("Método de envío no válido."));

        DireccionEnvio direccion = null;
        if (Boolean.TRUE.equals(metodo.getRequiereDireccion())) {
            if (direccionId == null) {
                throw new IllegalArgumentException("Debe seleccionar una dirección de entrega.");
            }
            direccion = direccionRepo.findById(direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada."));
            if (!direccion.getCliente().getId().equals(cliente.getId())) {
                throw new IllegalArgumentException("No autorizado.");
            }
        }

        BigDecimal subtotal = calcularSubtotal(items);
        BigDecimal costoEnvio = metodo.getCostoBase();

        Pedido pedido = new Pedido();
        pedido.setNumeroOrden("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("pendiente");
        pedido.setSubtotal(subtotal);
        pedido.setCostoEnvio(costoEnvio);
        pedido.setDescuentoTotal(BigDecimal.ZERO);
        pedido.setTotal(subtotal.add(costoEnvio));
        pedido.setMetodoEnvio(metodo);
        pedido.setDireccionEnvio(direccion);
        pedidoRepo.save(pedido);

        for (ItemCarrito item : items) {
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(item.getProducto());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getProducto().getPrecio());
            detalleRepo.save(detalle);
        }

        limpiarCarrito(carrito);
        return pedido;
    }
}
