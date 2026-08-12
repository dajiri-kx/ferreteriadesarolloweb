package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.domain.Pedido;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.domain.Pago;
import com.proyecto.toolboxcr.service.PedidoService;
import com.proyecto.toolboxcr.service.PagoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pago")
public class PagoController {

    @Autowired private PedidoService pedidoService;
    @Autowired private PagoService pagoService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    private Usuario getUsuario(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    /* CC-04 — Mostrar la selección de pasarela de pago */
    @GetMapping("/gateway/{pedidoId}")
    public String gateway(@PathVariable Long pedidoId, HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(pedidoId, usuario);

            if (!"pendiente".equals(pedido.getEstado())) {
                return "redirect:/perfil/pedidos/" + pedido.getId();
            }

            model.addAttribute("pedido", pedido);
            return "pago/gateway";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    /* CC-04 — Redirigir a la pasarela real de Stripe Checkout */
    @GetMapping("/stripe/{pedidoId}")
    public String stripeCheckout(@PathVariable Long pedidoId, HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(pedidoId, usuario);

            if (!"pendiente".equals(pedido.getEstado())) {
                return "redirect:/perfil/pedidos/" + pedido.getId();
            }

            // Si la clave de Stripe es nula, vacía o un placeholder sin resolver, usar la simulación local
            if (stripeApiKey == null || stripeApiKey.trim().isEmpty() || stripeApiKey.startsWith("${")) {
                model.addAttribute("pedido", pedido);
                return "pago/stripe_checkout";
            }

            // Configurar Stripe API Key
            com.stripe.Stripe.apiKey = stripeApiKey;

            // Construir URL base para los retornos
            String successUrl = "http://localhost:9202/pago/stripe/retorno/" + pedidoId;
            String cancelUrl = "http://localhost:9202/pago/fallo/" + pedidoId + "?motivo=Pago cancelado por el usuario";

            com.stripe.param.checkout.SessionCreateParams params =
                com.stripe.param.checkout.SessionCreateParams.builder()
                    .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                        com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("crc")
                                    .setUnitAmount(pedido.getTotal().multiply(new java.math.BigDecimal(100)).longValue())
                                    .setProductData(
                                        com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Pedido " + pedido.getNumeroOrden())
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build();

            com.stripe.model.checkout.Session stripeSession = com.stripe.model.checkout.Session.create(params);
            return "redirect:" + stripeSession.getUrl();
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Error al inicializar sesión de Stripe: " + e.getMessage());
            return "redirect:/carrito";
        }
    }

    /* CC-04 — Retorno exitoso de Stripe Checkout */
    @GetMapping("/stripe/retorno/{pedidoId}")
    public String retornoStripe(@PathVariable Long pedidoId, HttpSession session, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(pedidoId, usuario);

            if ("pendiente".equals(pedido.getEstado())) {
                pagoService.procesarPagoSimulado(pedido, "stripe", true, null);
                // Actualizar badge del carrito en sesión a 0
                session.setAttribute("carritoCount", 0);
            }

            return "redirect:/pago/exito/" + pedidoId;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }


    /* CC-04 — Procesar el pago simulado de Stripe */
    @PostMapping("/stripe/procesar")
    public String procesarStripe(@RequestParam Long pedidoId,
                                 @RequestParam boolean simularExito,
                                 @RequestParam(required = false, defaultValue = "Fondos insuficientes") String motivoFallo,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(pedidoId, usuario);

            if (!"pendiente".equals(pedido.getEstado())) {
                return "redirect:/perfil/pedidos/" + pedido.getId();
            }

            Pago pago = pagoService.procesarPagoSimulado(pedido, "stripe", simularExito, motivoFallo);

            if (simularExito) {
                // Actualizar badge del carrito en sesión a 0
                session.setAttribute("carritoCount", 0);
                return "redirect:/pago/exito/" + pedido.getId();
            } else {
                return "redirect:/pago/fallo/" + pedido.getId() + "?motivo=" + motivoFallo;
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    /* CC-04 — Procesar el pago simulado de SINPE Móvil */
    @PostMapping("/sinpe/procesar")
    public String procesarSinpe(@RequestParam Long pedidoId,
                                @RequestParam String comprobante,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(pedidoId, usuario);

            if (!"pendiente".equals(pedido.getEstado())) {
                return "redirect:/perfil/pedidos/" + pedido.getId();
            }

            if (comprobante == null || comprobante.trim().isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Debe ingresar el número de comprobante SINPE.");
                return "redirect:/pago/gateway/" + pedidoId;
            }

            pagoService.procesarPagoSimulado(pedido, "transferencia_sinpe", true, null);
            session.setAttribute("carritoCount", 0);

            return "redirect:/pago/exito/" + pedido.getId();
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    /* CC-04 — Mostrar pantalla de éxito */
    @GetMapping("/exito/{pedidoId}")
    public String exito(@PathVariable Long pedidoId, HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(pedidoId, usuario);
            model.addAttribute("pedido", pedido);
            return "pago/exito";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    /* CC-04 — Mostrar pantalla de fallo con opción de reintentar */
    @GetMapping("/fallo/{pedidoId}")
    public String fallo(@PathVariable Long pedidoId,
                        @RequestParam(required = false, defaultValue = "Error en el procesamiento del pago.") String motivo,
                        HttpSession session,
                        Model model,
                        RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = getUsuario(session);
            Pedido pedido = pedidoService.getDetalle(pedidoId, usuario);
            model.addAttribute("pedido", pedido);
            model.addAttribute("motivo", motivo);
            return "pago/fallo";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
}
