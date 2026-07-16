package com.fastmarket.api.pattern.behavioral.observer;

import com.fastmarket.api.model.EstadoPedido;
import com.fastmarket.api.model.Pedido;
import com.fastmarket.api.model.Usuario;

/** Evento inmutable emitido cuando se crea un pedido o cambia su estado. */
public record PedidoEvento(
        Pedido pedido,
        EstadoPedido estadoAnterior,
        EstadoPedido estadoNuevo,
        Usuario actor,
        String motivo
) {}
