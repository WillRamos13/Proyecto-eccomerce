package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

public final class PendienteState extends AbstractPedidoState {
    public PendienteState() {
        super(EstadoPedido.PENDIENTE, EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO);
    }
}
