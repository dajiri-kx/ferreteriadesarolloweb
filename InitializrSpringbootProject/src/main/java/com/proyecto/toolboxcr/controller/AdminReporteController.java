package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.service.PedidoService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController {

    @Autowired
    private PedidoService pedidoService;

    // A-05, Reporte de ventas 
    @GetMapping("/ventas")
    public String ventas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model model) {

        if (fechaInicio == null) {
            fechaInicio = LocalDate.now().withDayOfMonth(1);
        }

        if (fechaFin == null) {
            fechaFin = LocalDate.now();
        }

        cargarDatosReporte(model, fechaInicio, fechaFin);

        return "admin/reportes/ventas";
    }

    //-05 — Exportación compatible con Excel
    @GetMapping("/ventas/excel")
    public void exportarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            HttpServletResponse response) throws IOException {

        List<Pedido> pedidos = pedidoService.getPedidosReporte(fechaInicio, fechaFin);
        List<Object[]> ranking = pedidoService.getRankingProductosVendidos(fechaInicio, fechaFin);

        BigDecimal ventas = pedidoService.getVentasPorPeriodo(fechaInicio, fechaFin);
        Long cantidadPedidos = pedidoService.getCantidadPedidosPorPeriodo(fechaInicio, fechaFin);
        BigDecimal ticketPromedio = pedidoService.getTicketPromedio(fechaInicio, fechaFin);

        response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_ventas.xls");

        StringBuilder html = new StringBuilder();

        html.append("<html><head><meta charset='UTF-8'></head><body>");
        html.append("<h2>Reporte de Ventas - ToolboxCR</h2>");
        html.append("<p>Periodo: ").append(fechaInicio).append(" al ").append(fechaFin).append("</p>");

        html.append("<h3>Resumen</h3>");
        html.append("<table border='1'>");
        html.append("<tr><th>Ventas totales</th><th>Cantidad de pedidos</th><th>Ticket promedio</th></tr>");
        html.append("<tr>");
        html.append("<td>").append(ventas).append("</td>");
        html.append("<td>").append(cantidadPedidos).append("</td>");
        html.append("<td>").append(ticketPromedio).append("</td>");
        html.append("</tr>");
        html.append("</table>");

        html.append("<h3>Pedidos</h3>");
        html.append("<table border='1'>");
        html.append("<tr><th>Orden</th><th>Cliente</th><th>Fecha</th><th>Estado</th><th>Total</th></tr>");

        for (Pedido pedido : pedidos) {
            html.append("<tr>");
            html.append("<td>").append(pedido.getNumeroOrden()).append("</td>");
            html.append("<td>").append(pedido.getCliente() != null ? pedido.getCliente().getNombre() : "Sin cliente").append("</td>");
            html.append("<td>").append(pedido.getFecha()).append("</td>");
            html.append("<td>").append(pedido.getEstado()).append("</td>");
            html.append("<td>").append(pedido.getTotal()).append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");

        html.append("<h3>Ranking de productos vendidos</h3>");
        html.append("<table border='1'>");
        html.append("<tr><th>Producto</th><th>Cantidad vendida</th><th>Ingresos</th></tr>");

        for (Object[] fila : ranking) {
            html.append("<tr>");
            html.append("<td>").append(fila[0]).append("</td>");
            html.append("<td>").append(fila[1]).append("</td>");
            html.append("<td>").append(fila[2]).append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");
        html.append("</body></html>");

        response.getWriter().write(html.toString());
    }

    private void cargarDatosReporte(Model model, LocalDate fechaInicio, LocalDate fechaFin) {
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        model.addAttribute("ventasDia", pedidoService.getVentasDelDia());
        model.addAttribute("ventasSemana", pedidoService.getVentasDeLaSemana());
        model.addAttribute("ventasMes", pedidoService.getVentasDelMes());

        model.addAttribute("ventasPeriodo", pedidoService.getVentasPorPeriodo(fechaInicio, fechaFin));
        model.addAttribute("cantidadPedidos", pedidoService.getCantidadPedidosPorPeriodo(fechaInicio, fechaFin));
        model.addAttribute("ticketPromedio", pedidoService.getTicketPromedio(fechaInicio, fechaFin));

        model.addAttribute("pedidos", pedidoService.getPedidosReporte(fechaInicio, fechaFin));
        model.addAttribute("rankingProductos", pedidoService.getRankingProductosVendidos(fechaInicio, fechaFin));
    }
}
