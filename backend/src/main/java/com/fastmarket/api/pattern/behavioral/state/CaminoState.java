package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

public final class CaminoState extends AbstractPedidoState {
    public CaminoState() {
        super(EstadoPedido.CAMINO, EstadoPedido.ENTREGADO);
    }
}
