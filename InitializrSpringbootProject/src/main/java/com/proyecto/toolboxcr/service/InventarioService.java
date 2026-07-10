package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.AlertaInventario;
import com.proyecto.toolboxcr.domain.Inventario;
import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.repositorio.AlertaInventarioRepository;
import com.proyecto.toolboxcr.repositorio.InventarioRepository;
import com.proyecto.toolboxcr.repositorio.ProductoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final AlertaInventarioRepository alertaInventarioRepository;
    private final ProductoRepository productoRepository;

    public InventarioService(InventarioRepository inventarioRepository,
                              AlertaInventarioRepository alertaInventarioRepository,
                              ProductoRepository productoRepository) {
        this.inventarioRepository = inventarioRepository;
        this.alertaInventarioRepository = alertaInventarioRepository;
        this.productoRepository = productoRepository;
    }

    // A-02: "El encargado de bodega quiere ver... el stock de cada producto"
    @Transactional(readOnly = true)
    public List<Inventario> listarInventario() {
        return inventarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Inventario> listarStockBajo() {
        return inventarioRepository.consultaStockBajo();
    }

    @Transactional(readOnly = true)
    public List<AlertaInventario> listarAlertasPendientes() {
        return alertaInventarioRepository.findByAtendidaFalseOrderByFechaAlertaDesc();
    }

    @Transactional(readOnly = true)
    public List<AlertaInventario> listarHistorialAlertas() {
        return alertaInventarioRepository.findAllByOrderByFechaAlertaDesc();
    }

    // A-02: "El encargado puede ajustar stock manualmente con registro del motivo."
    @Transactional
    public void ajustarStock(Long idProducto, Integer nuevoStock, Integer nuevoUmbral, String motivo) {
        if (nuevoStock == null || nuevoStock < 0) {
            throw new IllegalArgumentException("La cantidad de stock no puede ser negativa.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el motivo del ajuste.");
        }

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe."));

        Optional<Inventario> existente = inventarioRepository.findByProducto(producto);
        Inventario inventario = existente.orElseGet(Inventario::new);

        if (existente.isEmpty()) {
            inventario.setProducto(producto);
            if (nuevoUmbral == null) {
                nuevoUmbral = 5; // mismo valor por defecto que usa la tabla en la BD
            }
        }

        inventario.setStockDisponible(nuevoStock);
        if (nuevoUmbral != null) {
            inventario.setUmbralMinimo(nuevoUmbral);
        }
        inventario.setMotivoAjuste(motivo);

        // Al hacer este UPDATE/INSERT, si el stock queda por debajo del umbral,
        // el trigger trg_inventario_alerta_stock_bajo (ya corrido en la BD)
        // inserta automáticamente el registro en ALERTA_INVENTARIO.
        inventarioRepository.save(inventario);
    }

    // Marca una alerta como revisada por el encargado de bodega
    @Transactional
    public void marcarAlertaAtendida(Long idAlerta) {
        AlertaInventario alerta = alertaInventarioRepository.findById(idAlerta)
                .orElseThrow(() -> new IllegalArgumentException("La alerta no existe."));
        alerta.setAtendida(true);
        alertaInventarioRepository.save(alerta);
    }

    // Método de apoyo para cuando se crea un producto nuevo: inicializa su
    // fila de inventario en 0 para que aparezca en el listado de stock.
    @Transactional
    public void inicializarInventario(Producto producto, Integer stockInicial, Integer umbral) {
        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setStockDisponible(stockInicial != null ? stockInicial : 0);
        inventario.setUmbralMinimo(umbral != null ? umbral : 5);
        inventarioRepository.save(inventario);
    }
}