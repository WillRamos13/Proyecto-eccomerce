package com.fastmarket.api.pattern;

import com.fastmarket.api.model.*;
import com.fastmarket.api.pattern.behavioral.observer.PedidoEventPublisher;
import com.fastmarket.api.pattern.creational.factory.PedidoStateFactory;
import com.fastmarket.api.repository.*;
import com.fastmarket.api.service.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoServiceReversionTest {

    @Test
    void cancelarDevuelveStockYNoPermiteDobleReversion() {
        PedidoRepository pedidoRepository = mock(PedidoRepository.class);
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        CuponService cuponService = mock(CuponService.class);
        PedidoEventPublisher publisher = mock(PedidoEventPublisher.class);
        Pedido pedido = pedidoConStock();
        Producto producto = pedido.getItems().get(0).getProducto();

        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(productoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(new Usuario("Admin", "admin@test.com", "hash", Rol.ADMIN)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        PedidoService service = new PedidoService(
                pedidoRepository, productoRepository, usuarioRepository, cuponService,
                mock(PedidoHistorialRepository.class), mock(CarritoService.class), mock(SystemConfigService.class),
                new PedidoStateFactory(), publisher, 30);
        AuthTokenService.TokenData admin = new AuthTokenService.TokenData(99L, "admin@test.com", Rol.ADMIN, Long.MAX_VALUE);

        var respuesta = service.actualizarEstado(admin, 1L, EstadoPedido.CANCELADO);
        assertEquals(EstadoPedido.CANCELADO, respuesta.estado());
        assertEquals(EstadoPago.CANCELADO, respuesta.estadoPago());
        assertEquals(5, producto.getStock());
        assertFalse(pedido.getStockDescontado());

        var segunda = service.actualizarEstado(admin, 1L, EstadoPedido.CANCELADO);
        assertEquals(5, producto.getStock());
        assertEquals(EstadoPedido.CANCELADO, segunda.estado());
    }

    private Pedido pedidoConStock() {
        Usuario cliente = new Usuario("Ana", "ana@test.com", "hash", Rol.CLIENTE);
        cliente.setId(7L);
        Producto producto = new Producto();
        producto.setId(5L);
        producto.setNombre("Producto");
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setStock(3);
        producto.setImagen("img/logo.png");

        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCodigo("PED-1");
        pedido.setUsuario(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setEstadoPago(EstadoPago.PENDIENTE);
        pedido.setMetodoPago(PedidoService.MERCADO_PAGO);
        pedido.setSubtotal(new BigDecimal("20.00"));
        pedido.setCostoEnvio(BigDecimal.ZERO);
        pedido.setDescuento(BigDecimal.ZERO);
        pedido.setTotal(new BigDecimal("20.00"));
        pedido.setStockDescontado(true);
        pedido.setCuponUsoActivo(false);
        pedido.setFecha(LocalDateTime.now());

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setProductoNombre("Producto");
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("10.00"));
        item.setSubtotal(new BigDecimal("20.00"));
        pedido.getItems().add(item);
        return pedido;
    }
}
