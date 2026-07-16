package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

public final class PreparandoState extends AbstractPedidoState {
    public PreparandoState() {
        super(EstadoPedido.PREPARANDO, EstadoPedido.CAMINO, EstadoPedido.CANCELADO);
    }
}
