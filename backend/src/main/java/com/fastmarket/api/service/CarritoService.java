package com.fastmarket.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fastmarket.api.dto.CarritoDtos;
import com.fastmarket.api.dto.CuponDtos;
import com.fastmarket.api.model.Carrito;
import com.fastmarket.api.model.CarritoItem;
import com.fastmarket.api.model.Producto;
import com.fastmarket.api.model.Usuario;
import com.fastmarket.api.repository.CarritoRepository;
import com.fastmarket.api.repository.ProductoRepository;
import com.fastmarket.api.repository.UsuarioRepository;

@Service
public class CarritoService {
    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final CuponService cuponService;

    public CarritoService(CarritoRepository carritoRepository, UsuarioRepository usuarioRepository, ProductoRepository productoRepository, CuponService cuponService) {
        this.carritoRepository = carritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.cuponService = cuponService;
    }

    @Transactional(readOnly = true)
    public CarritoDtos.CarritoResponse obtener(Long usuarioId) {
        Carrito carrito = carritoRepository.findByUsuarioId(usuarioId).orElse(null);
        if (carrito == null) return respuestaVacia(usuarioId);
        return toResponse(carrito);
    }

    @Transactional
    public CarritoDtos.CarritoResponse sincronizar(Long usuarioId, CarritoDtos.SincronizarCarritoRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Carrito carrito = carritoRepository.findByUsuarioId(usuarioId).orElseGet(() -> {
            Carrito c = new Carrito();
            c.setUsuario(usuario);
            return c;
        });

        carrito.getItems().clear();
        carritoRepository.saveAndFlush(carrito);
        Map<Long, Integer> cantidadesPorProducto = agruparItems(request == null ? null : request.items());
        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productoRepository.findById(entry.getKey()).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            if (!Boolean.TRUE.equals(producto.getActivo())) continue;
            int cantidad = Math.min(entry.getValue(), Math.max(0, producto.getStock()));
            if (cantidad <= 0) continue;
            CarritoItem item = new CarritoItem();
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(producto.getPrecio());
            carrito.getItems().add(item);
        }
        String cupon = request == null ? null : request.cuponCodigo();
        carrito.setCuponCodigo(cupon == null || cupon.isBlank() ? null : cupon.trim().toUpperCase());
        carrito.setActualizadoEn(LocalDateTime.now());
        return toResponse(carritoRepository.save(carrito));
    }

    @Transactional
    public void limpiar(Long usuarioId) {
        carritoRepository.findByUsuarioId(usuarioId).ifPresent(carrito -> {
            carrito.getItems().clear();
            carrito.setCuponCodigo(null);
            carrito.setActualizadoEn(LocalDateTime.now());
            carritoRepository.save(carrito);
        });
    }

    private Map<Long, Integer> agruparItems(List<CarritoDtos.CarritoItemRequest> items) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        if (items == null) return cantidades;
        for (CarritoDtos.CarritoItemRequest item : items) {
            if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) continue;
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }
        return cantidades;
    }

    private CarritoDtos.CarritoResponse toResponse(Carrito carrito) {
        List<CarritoDtos.CarritoItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CarritoItem item : carrito.getItems()) {
            Producto p = item.getProducto();
            BigDecimal precio = p != null ? p.getPrecio() : item.getPrecioUnitario();
            BigDecimal itemSubtotal = precio.multiply(BigDecimal.valueOf(item.getCantidad())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemSubtotal);
            items.add(new CarritoDtos.CarritoItemResponse(
                    p != null ? p.getId() : null,
                    p != null ? p.getNombre() : "Producto",
                    p != null ? p.getImagen() : null,
                    precio,
                    item.getCantidad(),
                    itemSubtotal,
                    p != null ? p.getStock() : 0
            ));
        }
        BigDecimal descuento = BigDecimal.ZERO;
        if (carrito.getCuponCodigo() != null && !carrito.getCuponCodigo().isBlank() && !items.isEmpty()) {
            try {
                List<CuponDtos.AplicarCuponItemRequest> itemsCupon = items.stream()
                        .map(i -> new CuponDtos.AplicarCuponItemRequest(i.productoId(), i.cantidad()))
                        .toList();
                descuento = cuponService.calcularDescuento(carrito.getCuponCodigo(), itemsCupon, carrito.getUsuario().getId()).descuento();
            } catch (RuntimeException ignored) {
                descuento = BigDecimal.ZERO;
            }
        }
        BigDecimal total = subtotal.subtract(descuento);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
        return new CarritoDtos.CarritoResponse(carrito.getUsuario().getId(), carrito.getCuponCodigo(), subtotal, descuento, total, carrito.getActualizadoEn(), items);
    }

    private CarritoDtos.CarritoResponse respuestaVacia(Long usuarioId) {
        return new CarritoDtos.CarritoResponse(usuarioId, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), List.of());
    }
}
