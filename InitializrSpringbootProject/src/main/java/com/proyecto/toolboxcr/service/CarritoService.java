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

import com.proyecto.toolboxcr.repositorio.CuponRepository;
import com.proyecto.toolboxcr.service.CuponService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CarritoService {

    @Autowired
    private CarritoRepository carritoRepo;
    @Autowired
    private ItemCarritoRepository itemRepo;
    @Autowired
    private MetodoEnvioRepository metodoEnvioRepo;
    @Autowired
    private DireccionEnvioRepository direccionRepo;
    @Autowired
    private PedidoRepository pedidoRepo;
    @Autowired
    private DetallePedidoRepository detalleRepo;
    @Autowired
    private ProductoRepository productoRepo;
    @Autowired
    private InventarioRepository inventarioRepo;
    @Autowired
    private CuponRepository cuponRepo;
    @Autowired
    private CuponService cuponService;

    @PersistenceContext
    private EntityManager entityManager;


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

    public BigDecimal calcularPesoTotal(List<ItemCarrito> items) {
        BigDecimal totalPeso = BigDecimal.ZERO;
        for (ItemCarrito item : items) {
            BigDecimal pesoProducto = item.getProducto().getPeso();
            totalPeso = totalPeso.add(pesoProducto.multiply(BigDecimal.valueOf(item.getCantidad())));
        }
        return totalPeso;
    }

    public BigDecimal calcularCostoEnvio(MetodoEnvio metodo, DireccionEnvio direccion, List<ItemCarrito> items) {
        if (metodo == null) {
            return BigDecimal.ZERO;
        }
        if (!Boolean.TRUE.equals(metodo.getRequiereDireccion())) {
            return BigDecimal.ZERO;
        }

        // Costo base del método
        BigDecimal costo = metodo.getCostoBase();

        // Costo base por dirección (segun provincia por código postal de Costa Rica: 1=SJ, 2=Alajuela, etc.)
        BigDecimal costoDireccion = BigDecimal.ZERO;
        if (direccion != null && direccion.getCodigoPostal() != null && !direccion.getCodigoPostal().trim().isEmpty()) {
            char primerDigito = direccion.getCodigoPostal().trim().charAt(0);
            switch (primerDigito) {
                case '1':
                    costoDireccion = new BigDecimal("1000.00");
                    break; // San José
                case '2':
                    costoDireccion = new BigDecimal("1500.00");
                    break; // Alajuela
                case '3':
                    costoDireccion = new BigDecimal("1500.00");
                    break; // Cartago
                case '4':
                    costoDireccion = new BigDecimal("1200.00");
                    break; // Heredia
                case '5':
                    costoDireccion = new BigDecimal("2500.00");
                    break; // Guanacaste
                case '6':
                    costoDireccion = new BigDecimal("2500.00");
                    break; // Puntarenas
                case '7':
                    costoDireccion = new BigDecimal("2500.00");
                    break; // Limón
                default:
                    costoDireccion = new BigDecimal("1500.00");
                    break;
            }
        }

        // Recargo por peso: peso total * 200 ₡
        BigDecimal pesoTotal = calcularPesoTotal(items);
        BigDecimal recargoPeso = pesoTotal.multiply(new BigDecimal("200.00"));

        return costo.add(costoDireccion).add(recargoPeso);
    }

    /* CC-03 — Crear pedido a partir del carrito sin vaciarlo inmediatamente */
    @Transactional
    public Pedido crearPedido(Usuario cliente, Long metodoEnvioId, Long direccionId, String cuponCodigo) {
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
        BigDecimal costoEnvio = calcularCostoEnvio(metodo, direccion, items);

        // Procesar cupón si existe
        Cupon cupon = null;
        BigDecimal descuento = BigDecimal.ZERO;
        if (cuponCodigo != null && !cuponCodigo.trim().isEmpty()) {
            cupon = cuponRepo.findByCodigoIgnoreCase(cuponCodigo.trim())
                    .orElseThrow(() -> new IllegalArgumentException("El código de cupón no es válido."));
            // Validar cupón
            cupon = cuponService.validarCupon(cuponCodigo, items);
            descuento = cuponService.calcularDescuento(cupon, items);

            BigDecimal descuentoAutomatico = cuponService.calcularDescuentoAutomaticoPorCantidad(items);
            descuento = descuento.add(descuentoAutomatico);

            if (descuento.compareTo(subtotal) > 0) {
                descuento = subtotal;
            }
        }

        Pedido pedido = new Pedido();
        pedido.setNumeroOrden("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("pendiente");
        pedido.setSubtotal(subtotal);
        pedido.setCostoEnvio(costoEnvio);
        pedido.setDescuentoTotal(descuento);

        BigDecimal descuentoAutomatico = cuponService.calcularDescuentoAutomaticoPorCantidad(items);
        descuento = descuento.add(descuentoAutomatico);

        if (descuento.compareTo(subtotal) > 0) {
            descuento = subtotal;
        }

        BigDecimal baseImponible = subtotal.subtract(descuento);
        if (baseImponible.compareTo(BigDecimal.ZERO) < 0) {
            baseImponible = BigDecimal.ZERO;
        }
        BigDecimal iva = baseImponible.multiply(new BigDecimal("0.13")).setScale(2, java.math.RoundingMode.HALF_UP);
        pedido.setTotal(subtotal.add(costoEnvio).subtract(descuento).add(iva));
        pedido.setMetodoEnvio(metodo);
        pedido.setDireccionEnvio(direccion);
        pedido.setCupon(cupon);
        pedidoRepo.save(pedido);

        // Si hay cupón, contrarrestar el incremento automático del trigger en BD si es que existe
        if (cupon != null) {
            int usosAntes = cupon.getUsosActuales();
            entityManager.flush(); // Forzar el INSERT del PEDIDO en la BD
            entityManager.refresh(cupon); // Recargar el cupón de la base de datos

            if (cupon.getUsosActuales() > usosAntes) {
                // El trigger de BD incrementó los usos automáticamente, por lo tanto lo decrementamos
                entityManager.createNativeQuery(
                        "UPDATE CUPON SET usos_actuales = usos_actuales - 1 WHERE id = :cuponId"
                )
                        .setParameter("cuponId", cupon.getId())
                        .executeUpdate();

                // Actualizar la entidad en memoria
                cupon.setUsosActuales(usosAntes);
            }
        }

        for (ItemCarrito item : items) {
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(item.getProducto());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getProducto().getPrecio());
            detalleRepo.save(detalle);
        }

        // NOTA: No limpiamos el carrito aquí para permitir al usuario editar el carrito.
        // Se limpiará en el flujo de Pago Exitoso.
        return pedido;
    }
}
