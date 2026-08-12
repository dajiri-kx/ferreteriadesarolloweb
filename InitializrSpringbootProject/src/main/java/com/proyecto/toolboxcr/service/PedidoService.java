package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.DetallePedido;
import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.repositorio.DetallePedidoRepository;
import com.proyecto.toolboxcr.repositorio.PedidoRepository;
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
}
