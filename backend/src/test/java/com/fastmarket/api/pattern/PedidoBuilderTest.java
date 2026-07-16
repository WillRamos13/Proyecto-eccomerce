package com.fastmarket.api.pattern;

import com.fastmarket.api.model.Producto;
import com.fastmarket.api.model.Rol;
import com.fastmarket.api.model.Usuario;
import com.fastmarket.api.pattern.creational.builder.PedidoBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PedidoBuilderTest {

    @Test
    void construyePedidoConItemsYTotalesConsistentes() {
        Usuario cliente = new Usuario("Ana", "ana@correo.com", "hash", Rol.CLIENTE);
        Producto producto = new Producto();
        producto.setNombre("Audífonos");
        producto.setPrecio(new BigDecimal("80.00"));

        var pedido = PedidoBuilder.nuevo(cliente)
                .conCodigo("PED-TEST-001")
                .conEntrega("Av. Principal 123", "Puerta azul", "9:00-12:00", "999999999")
                .conMetodoPago("Mercado Pago")
                .agregarProducto(producto, 2)
                .conTotales(new BigDecimal("160.00"), new BigDecimal("8.00"), new BigDecimal("10.00"))
                .build();

        assertEquals(new BigDecimal("158.00"), pedido.getTotal());
        assertEquals(1, pedido.getItems().size());
        assertEquals(pedido, pedido.getItems().get(0).getPedido());
    }

    @Test
    void rechazaPedidoSinProductos() {
        Usuario cliente = new Usuario("Ana", "ana@correo.com", "hash", Rol.CLIENTE);
        PedidoBuilder builder = PedidoBuilder.nuevo(cliente)
                .conCodigo("PED-TEST-002")
                .conTotales(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThrows(IllegalStateException.class, builder::build);
    }
}
