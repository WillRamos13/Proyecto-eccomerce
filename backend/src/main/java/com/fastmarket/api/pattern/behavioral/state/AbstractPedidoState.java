package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public abstract class AbstractPedidoState implements EstadoPedidoState {
    private final EstadoPedido estado;
    private final Set<EstadoPedido> permitidos;

    protected AbstractPedidoState(EstadoPedido estado, EstadoPedido... permitidos) {
        this.estado = estado;
        if (permitidos.length == 0) {
            this.permitidos = Collections.emptySet();
        } else {
            EnumSet<EstadoPedido> conjunto = EnumSet.noneOf(EstadoPedido.class);
            Collections.addAll(conjunto, permitidos);
            this.permitidos = Collections.unmodifiableSet(conjunto);
        }
    }

    @Override
    public EstadoPedido estado() {
        return estado;
    }

    @Override
    public Set<EstadoPedido> transicionesPermitidas() {
        return permitidos;
    }
}
