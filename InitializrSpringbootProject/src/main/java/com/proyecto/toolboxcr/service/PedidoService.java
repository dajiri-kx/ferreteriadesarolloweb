package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.DetallePedido;
import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.DetallePedidoRepository;
import com.proyecto.toolboxcr.repositorio.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepo;

    @Autowired
    private DetallePedidoRepository detalleRepo;

    /* CU-04 — Historial de pedidos */
    public List<Pedido> getHistorial(Usuario cliente) {
        return pedidoRepo.findByClienteIdOrderByFechaDesc(cliente.getId());
    }

    /* CU-04 — Detalle de un pedido */
    public Pedido getDetalle(Long idPedido, Usuario cliente) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        if (!pedido.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("No autorizado.");
        }
        return pedido;
    }

    /* CU-04 — Líneas del pedido */
    public List<DetallePedido> getDetalles(Pedido pedido) {
        return detalleRepo.findByPedido(pedido);
    }

    /* CC-05 — Cancelar un pedido pendiente (ej. si vuelve a editar el carrito) */
    @org.springframework.transaction.annotation.Transactional
    public void cancelarPedido(Long idPedido, Usuario cliente) {
        Pedido pedido = getDetalle(idPedido, cliente);
        if ("pendiente".equals(pedido.getEstado())) {
            pedido.setEstado("cancelado");
            pedidoRepo.save(pedido);
        }
    }

    /* A-03 — Listado administrativo con filtros por estado y fecha */
    public List<Pedido> getPedidosAdministracion(String estado, LocalDate fechaInicio, LocalDate fechaFin) {
        boolean tieneEstado = estado != null && !estado.isBlank();
        boolean tieneFechas = fechaInicio != null && fechaFin != null;

        if (tieneEstado && tieneFechas) {
            return pedidoRepo.findByEstadoAndFechaBetweenOrderByFechaDesc(
                    estado,
                    fechaInicio.atStartOfDay(),
                    fechaFin.atTime(LocalTime.MAX)
            );
        }

        if (tieneEstado) {
            return pedidoRepo.findByEstadoOrderByFechaDesc(estado);
        }

        if (tieneFechas) {
            return pedidoRepo.findByFechaBetweenOrderByFechaDesc(
                    fechaInicio.atStartOfDay(),
                    fechaFin.atTime(LocalTime.MAX)
            );
        }

        return pedidoRepo.findAllByOrderByFechaDesc();
    }

    /* A-03 — Detalle administrativo de pedido */
    public Pedido getPedidoAdministracion(Long idPedido) {
        return pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
    }

    /* A-03 — Actualización administrativa del estado */
    @org.springframework.transaction.annotation.Transactional
    public void actualizarEstadoAdministracion(Long idPedido, String nuevoEstado) {
        if (!esEstadoValido(nuevoEstado)) {
            throw new IllegalArgumentException("Estado de pedido no válido.");
        }

        Pedido pedido = getPedidoAdministracion(idPedido);
        pedido.setEstado(nuevoEstado);
        pedidoRepo.save(pedido);
    }

    /* A-03 — Estados permitidos*/
    public List<String> getEstadosAdministracion() {
        return List.of("pendiente", "preparando", "enviado", "entregado");
    }

    private boolean esEstadoValido(String estado) {
        return getEstadosAdministracion().contains(estado);
    }

    /* A-05 — Ventas totales por periodo */
    public BigDecimal getVentasPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        return pedidoRepo.sumarVentasPorPeriodo(
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX)
        );
    }

    /* A-05 — Cantidad de pedidos por periodo */
    public Long getCantidadPedidosPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        return pedidoRepo.contarPedidosPorPeriodo(
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX)
        );
    }

    /* A-05 — Ticket promedio por pedido */
    public BigDecimal getTicketPromedio(LocalDate fechaInicio, LocalDate fechaFin) {
        BigDecimal ventas = getVentasPorPeriodo(fechaInicio, fechaFin);
        Long cantidad = getCantidadPedidosPorPeriodo(fechaInicio, fechaFin);

        if (cantidad == null || cantidad == 0) {
            return BigDecimal.ZERO;
        }

        return ventas.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    /* A-05 — Pedidos incluidos en el reporte */
    public List<Pedido> getPedidosReporte(LocalDate fechaInicio, LocalDate fechaFin) {
        return pedidoRepo.buscarPedidosReporte(
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX)
        );
    }

    /* A-05 — Ranking de productos vendidos */
    public List<Object[]> getRankingProductosVendidos(LocalDate fechaInicio, LocalDate fechaFin) {
        return detalleRepo.rankingProductosVendidos(
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX)
        );
    }

    /* A-05 — Venta del día actual */
    public BigDecimal getVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        return getVentasPorPeriodo(hoy, hoy);
    }

    /* A-05 — Venta de la semana actual */
    public BigDecimal getVentasDeLaSemana() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
        return getVentasPorPeriodo(inicioSemana, hoy);
    }

    /* A-05 — Venta del mes actual */
    public BigDecimal getVentasDelMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        return getVentasPorPeriodo(inicioMes, hoy);
    }

}
