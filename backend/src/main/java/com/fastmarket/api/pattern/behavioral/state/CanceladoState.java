package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

public final class CanceladoState extends AbstractPedidoState {
    public CanceladoState() {
        super(EstadoPedido.CANCELADO);
    }
}
