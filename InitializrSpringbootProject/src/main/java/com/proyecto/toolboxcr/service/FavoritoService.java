package com.proyecto.toolboxcr.service;

import com.proyecto.toolboxcr.domain.Favorito;
import com.proyecto.toolboxcr.domain.Usuario;
import com.proyecto.toolboxcr.domain.Producto;
import com.proyecto.toolboxcr.repositorio.FavoritoRepository;
import com.proyecto.toolboxcr.repositorio.ProductoRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoritoService {

    @Autowired
    private FavoritoRepository favoritoRepo;

    @Autowired
    private ProductoRepository productoRepo;

    @Autowired
    private CarritoService carritoService;

    @Transactional(readOnly = true)
    public List<Favorito> listar(Usuario cliente) {
        return favoritoRepo.findByCliente(cliente);
    }

    @Transactional(readOnly = true)
    public Set<Long> obtenerFavoritosIdsPorCliente(Usuario cliente) {
        return favoritoRepo.findByCliente(cliente).stream()
                .map(f -> f.getProducto().getId())
                .collect(Collectors.toSet());
    }

    @Transactional
    public boolean toggle(Usuario cliente, Long productoId) {
        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        var favoritoOpt = favoritoRepo.findByClienteAndProducto(cliente, producto);
        if (favoritoOpt.isPresent()) {
            favoritoRepo.delete(favoritoOpt.get());
            return false; // Removed
        } else {
            Favorito f = new Favorito();
            f.setCliente(cliente);
            f.setProducto(producto);
            favoritoRepo.save(f);
            return true; // Added
        }
    }

    @Transactional
    public void moverAlCarrito(Usuario cliente, Long productoId) {
        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        // 1. Agregar al carrito (cantidad 1 por defecto)
        carritoService.agregarItem(cliente, productoId, 1);

        // 2. Eliminar de favoritos
        var favoritoOpt = favoritoRepo.findByClienteAndProducto(cliente, producto);
        favoritoOpt.ifPresent(favorito -> favoritoRepo.delete(favorito));
    }
}
