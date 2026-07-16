package com.fastmarket.api.pattern.behavioral.state;

import com.fastmarket.api.model.EstadoPedido;

import java.util.Set;

/** Patrón State: cada estado encapsula sus transiciones válidas. */
public interface EstadoPedidoState {
    EstadoPedido estado();
    Set<EstadoPedido> transicionesPermitidas();

    default void validarTransicion(EstadoPedido destino) {
        if (destino == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }
        if (destino == estado()) {
            return;
        }
        if (!transicionesPermitidas().contains(destino)) {
            throw new IllegalStateException(
                    "Transición no permitida: " + estado() + " -> " + destino
            );
        }
    }
}
