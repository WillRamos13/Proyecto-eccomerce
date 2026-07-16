package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

public final class ConfirmadoState extends AbstractPedidoState {
    public ConfirmadoState() {
        super(EstadoPedido.CONFIRMADO, EstadoPedido.PREPARANDO, EstadoPedido.CANCELADO);
    }
}
