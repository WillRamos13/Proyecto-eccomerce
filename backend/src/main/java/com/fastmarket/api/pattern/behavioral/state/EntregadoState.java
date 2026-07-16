package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

public final class EntregadoState extends AbstractPedidoState {
    public EntregadoState() {
        super(EstadoPedido.ENTREGADO);
    }
}
