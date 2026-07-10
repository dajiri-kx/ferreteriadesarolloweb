package com.proyecto.toolboxcr.controller;

import com.proyecto.toolboxcr.service.InventarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/listado")
    public String listado(Model model) {
        model.addAttribute("inventarios", inventarioService.listarInventario());
        model.addAttribute("stockBajo", inventarioService.listarStockBajo());
        model.addAttribute("alertasPendientes", inventarioService.listarAlertasPendientes());
        return "inventario/listado";
    }

    @PostMapping("/ajustar")
    public String ajustar(@RequestParam Long idProducto,
                           @RequestParam Integer nuevoStock,
                           @RequestParam(required = false) Integer nuevoUmbral,
                           @RequestParam String motivo,
                           RedirectAttributes redirectAttrs) {
        try {
            inventarioService.ajustarStock(idProducto, nuevoStock, nuevoUmbral, motivo);
            redirectAttrs.addFlashAttribute("todoOk", "Stock actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventario/listado";
    }

    @PostMapping("/atenderAlerta")
    public String atenderAlerta(@RequestParam Long idAlerta, RedirectAttributes redirectAttrs) {
        inventarioService.marcarAlertaAtendida(idAlerta);
        redirectAttrs.addFlashAttribute("todoOk", "Alerta marcada como revisada.");
        return "redirect:/inventario/listado";
    }
}