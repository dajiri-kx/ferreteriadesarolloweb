package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Carrito;
import com.proyecto.toolboxcr.domain.ItemCarrito;
import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.domain.MetodoEnvio;
import com.proyecto.toolboxcr.domain.DireccionEnvio;
import com.proyecto.toolboxcr.domain.Cupon;
import com.proyecto.toolboxcr.repositorio.MetodoEnvioRepository;
import com.proyecto.toolboxcr.repositorio.DireccionEnvioRepository;
import com.proyecto.toolboxcr.repositorio.CuponRepository;
import com.proyecto.toolboxcr.service.CarritoService;
import com.proyecto.toolboxcr.service.DireccionService;
import com.proyecto.toolboxcr.service.PedidoService;
import com.proyecto.toolboxcr.service.CuponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired private CarritoService carritoService;
    @Autowired private MetodoEnvioRepository metodoEnvioRepo;
    @Autowired private DireccionService direccionService;
    @Autowired private DireccionEnvioRepository direccionEnvioRepo;
    @Autowired private PedidoService pedidoService;
    @Autowired private CuponRepository cuponRepo;
    @Autowired private CuponService cuponService;

    private Usuario getUsuario(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    private void actualizarBadge(HttpSession session, Usuario usuario) {
        Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
        session.setAttribute("carritoCount", carritoService.contarItems(carrito));
    }

    /* CC-01 — Ver carrito */
    @GetMapping
    public String verCarrito(HttpSession session, Model model) {
        Usuario usuario = getUsuario(session);
        Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
        List<ItemCarrito> items = carritoService.obtenerItems(carrito);
        BigDecimal subtotal = carritoService.calcularSubtotal(items);

        // Calcular descuento si hay un cupón activo en la sesión
        BigDecimal descuento = BigDecimal.ZERO;
        String cuponCodigo = (String) session.getAttribute("cuponAplicado");
        Cupon cupon = null;
        if (cuponCodigo != null) {
            try {
                cupon = cuponRepo.findByCodigoIgnoreCase(cuponCodigo.trim()).orElse(null);
                if (cupon != null) {
                    cupon = cuponService.validarCupon(cuponCodigo, items);
                    descuento = cuponService.calcularDescuento(cupon, items);
                    model.addAttribute("cuponExito", "Cupón '" + cupon.getCodigo() + "' aplicado con éxito.");
                } else {
                    session.removeAttribute("cuponAplicado");
                }
            } catch (Exception e) {
                session.removeAttribute("cuponAplicado");
                model.addAttribute("cuponError", e.getMessage());
            }
        }

        BigDecimal baseImponible = subtotal.subtract(descuento);
        if (baseImponible.compareTo(BigDecimal.ZERO) < 0) {
            baseImponible = BigDecimal.ZERO;
        }
        BigDecimal iva = baseImponible.multiply(new BigDecimal("0.13")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalConDescuento = baseImponible.add(iva);

        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("descuento", descuento);
        model.addAttribute("iva", iva);
        model.addAttribute("cuponAplicado", cupon);
        model.addAttribute("totalConDescuento", totalConDescuento);

        actualizarBadge(session, usuario);
        return "carrito/carrito";
    }

    /* CC-01 — Agregar producto al carrito */
    @PostMapping("/agregar")
    public String agregar(@RequestParam Long productoId,
                          @RequestParam(defaultValue = "1") int cantidad,
                          HttpSession session,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttrs) {
        try {
            carritoService.agregarItem(getUsuario(session), productoId, cantidad);
            actualizarBadge(session, getUsuario(session));
            redirectAttrs.addFlashAttribute("todoOk", "Producto agregado al carrito.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    /* CC-01 — Actualizar cantidad de un ítem */
    @PostMapping("/actualizar")
    public String actualizar(@RequestParam Long itemId,
                             @RequestParam int cantidad,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        try {
            carritoService.actualizarCantidad(getUsuario(session), itemId, cantidad);
            actualizarBadge(session, getUsuario(session));
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    /* CC-01 — Eliminar ítem */
    @PostMapping("/eliminar")
    public String eliminar(@RequestParam Long itemId,
                           HttpSession session,
                           RedirectAttributes redirectAttrs) {
        try {
            carritoService.eliminarItem(getUsuario(session), itemId);
            actualizarBadge(session, getUsuario(session));
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    @PostMapping("/aplicar-cupon")
    public String aplicarCupon(@RequestParam String cuponCodigo, HttpSession session, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
            List<ItemCarrito> items = carritoService.obtenerItems(carrito);

            // Validar cupón
            Cupon cupon = cuponService.validarCupon(cuponCodigo, items);
            session.setAttribute("cuponAplicado", cupon.getCodigo());
            redirectAttrs.addFlashAttribute("todoOk", "Cupón aplicado con éxito.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    @GetMapping("/remover-cupon")
    public String removerCupon(HttpSession session, RedirectAttributes redirectAttrs) {
        session.removeAttribute("cuponAplicado");
        redirectAttrs.addFlashAttribute("todoOk", "Cupón removido.");
        return "redirect:/carrito";
    }

    /* CC-03 — Mostrar checkout */
    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model) {
        Usuario usuario = getUsuario(session);
        Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
        List<ItemCarrito> items = carritoService.obtenerItems(carrito);

        if (items.isEmpty()) {
            return "redirect:/carrito";
        }

        BigDecimal subtotal = carritoService.calcularSubtotal(items);

        // Calcular descuento
        BigDecimal descuento = BigDecimal.ZERO;
        String cuponCodigo = (String) session.getAttribute("cuponAplicado");
        Cupon cupon = null;
        if (cuponCodigo != null) {
            try {
                cupon = cuponRepo.findByCodigoIgnoreCase(cuponCodigo.trim()).orElse(null);
                if (cupon != null) {
                    cupon = cuponService.validarCupon(cuponCodigo, items);
                    descuento = cuponService.calcularDescuento(cupon, items);
                }
            } catch (Exception e) {
                session.removeAttribute("cuponAplicado");
            }
        }

        BigDecimal baseImponible = subtotal.subtract(descuento);
        if (baseImponible.compareTo(BigDecimal.ZERO) < 0) {
            baseImponible = BigDecimal.ZERO;
        }
        BigDecimal iva = baseImponible.multiply(new BigDecimal("0.13")).setScale(2, java.math.RoundingMode.HALF_UP);

        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("descuento", descuento);
        model.addAttribute("iva", iva);
        model.addAttribute("cuponAplicado", cupon);
        model.addAttribute("metodos", metodoEnvioRepo.findAll());
        model.addAttribute("direcciones", direccionService.listar(usuario));
        return "carrito/checkout";
    }

    /* CC-03 — Confirmar pedido */
    @PostMapping("/confirmar")
    public String confirmar(@RequestParam Long metodoEnvioId,
                            @RequestParam(required = false) Long direccionId,
                            HttpSession session,
                            RedirectAttributes redirectAttrs) {
        try {
            String cuponCodigo = (String) session.getAttribute("cuponAplicado");
            Pedido pedido = carritoService.crearPedido(getUsuario(session), metodoEnvioId, direccionId, cuponCodigo);
            
            // Remover el cupón de la sesión ya que el pedido ya fue creado y lo incluye
            session.removeAttribute("cuponAplicado");
            
            return "redirect:/carrito/resumen/" + pedido.getId();
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito/checkout";
        }
    }

    /* CC-03 — Calcular costo de envío dinámicamente vía REST/AJAX */
    @GetMapping("/calcular-envio")
    @ResponseBody
    public Map<String, Object> calcularEnvio(@RequestParam Long metodoEnvioId,
                                             @RequestParam(required = false) Long direccionId,
                                             HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Usuario usuario = getUsuario(session);
            Carrito carrito = carritoService.obtenerOCrearCarrito(usuario);
            List<ItemCarrito> items = carritoService.obtenerItems(carrito);

            BigDecimal subtotal = carritoService.calcularSubtotal(items);
            BigDecimal pesoTotal = carritoService.calcularPesoTotal(items);

            MetodoEnvio metodo = metodoEnvioRepo.findById(metodoEnvioId).orElse(null);
            DireccionEnvio direccion = null;
            if (direccionId != null) {
                direccion = direccionEnvioRepo.findById(direccionId).orElse(null);
            }

            BigDecimal costoEnvio = carritoService.calcularCostoEnvio(metodo, direccion, items);
            
            // Calcular descuento si hay cupón en sesión
            BigDecimal descuento = BigDecimal.ZERO;
            String cuponCodigo = (String) session.getAttribute("cuponAplicado");
            if (cuponCodigo != null) {
                try {
                    Cupon cupon = cuponRepo.findByCodigoIgnoreCase(cuponCodigo.trim()).orElse(null);
                    if (cupon != null) {
                        cupon = cuponService.validarCupon(cuponCodigo, items);
                        descuento = cuponService.calcularDescuento(cupon, items);
                    }
                } catch (Exception e) {
                    session.removeAttribute("cuponAplicado");
                }
            }

            BigDecimal baseImponible = subtotal.subtract(descuento);
            if (baseImponible.compareTo(BigDecimal.ZERO) < 0) {
                baseImponible = BigDecimal.ZERO;
            }
            BigDecimal iva = baseImponible.multiply(new BigDecimal("0.13")).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(costoEnvio).subtract(descuento).add(iva);

            response.put("success", true);
            response.put("subtotal", subtotal);
            response.put("descuento", descuento);
            response.put("iva", iva);
            response.put("pesoTotal", pesoTotal);
            response.put("costoEnvio", costoEnvio);
            response.put("total", total);
            response.put("requiereDireccion", metodo != null && metodo.getRequiereDireccion());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /* CC-05 — Mostrar resumen de pedido antes del pago */
    @GetMapping("/resumen/{id}")
    public String resumenPedido(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(id, usuario);
            System.out.println("[DEBUG CONTROLLER] Pedido ID " + id + " has cupon: " + (pedido.getCupon() != null ? pedido.getCupon().getCodigo() : "null"));

            // Solo mostrar resumen si el pedido está pendiente de pago
            if (!"pendiente".equals(pedido.getEstado())) {
                return "redirect:/perfil/pedidos/" + pedido.getId();
            }

            model.addAttribute("pedido", pedido);
            model.addAttribute("detalles", pedidoService.getDetalles(pedido));
            return "carrito/resumen";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Error al obtener resumen: " + e.getMessage());
            return "redirect:/carrito";
        }
    }

    /* CC-05 — Cancelar pedido y volver al carrito para editarlo */
    @PostMapping("/cancelar-pedido/{id}")
    public String cancelarPedido(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            pedidoService.cancelarPedido(id, usuario);
            redirectAttrs.addFlashAttribute("todoOk", "Pedido cancelado. Puedes editar tu carrito.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Error al cancelar pedido: " + e.getMessage());
        }
        return "redirect:/carrito";
    }
}
