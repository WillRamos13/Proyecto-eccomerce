package com.fastmarket.api.pattern;

import com.fastmarket.api.model.EstadoPedido;
import com.fastmarket.api.pattern.creational.factory.PedidoStateFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PedidoStateFactoryTest {
    private final PedidoStateFactory factory = new PedidoStateFactory();

    @Test
    void permiteFlujoProgresivoCompleto() {
        assertDoesNotThrow(() -> factory.crear(EstadoPedido.PENDIENTE).validarTransicion(EstadoPedido.CONFIRMADO));
        assertDoesNotThrow(() -> factory.crear(EstadoPedido.CONFIRMADO).validarTransicion(EstadoPedido.PREPARANDO));
        assertDoesNotThrow(() -> factory.crear(EstadoPedido.PREPARANDO).validarTransicion(EstadoPedido.CAMINO));
        assertDoesNotThrow(() -> factory.crear(EstadoPedido.CAMINO).validarTransicion(EstadoPedido.ENTREGADO));
    }

    @Test
    void impideSaltarEtapas() {
        assertThrows(IllegalStateException.class, () -> factory.crear(EstadoPedido.PENDIENTE).validarTransicion(EstadoPedido.ENTREGADO));
        assertThrows(IllegalStateException.class, () -> factory.crear(EstadoPedido.CONFIRMADO).validarTransicion(EstadoPedido.CAMINO));
        assertThrows(IllegalStateException.class, () -> factory.crear(EstadoPedido.PREPARANDO).validarTransicion(EstadoPedido.ENTREGADO));
    }

    @Test
    void impideReabrirUnPedidoEntregado() {
        assertThrows(IllegalStateException.class, () -> factory.crear(EstadoPedido.ENTREGADO).validarTransicion(EstadoPedido.PENDIENTE));
    }
}
