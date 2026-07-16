package com.fastmarket.api.pattern.creational.factory;

import com.fastmarket.api.model.EstadoPedido;
import com.fastmarket.api.pattern.behavioral.state.*;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Patrón Factory: centraliza la creación/selección del objeto State que
 * representa el estado actual de un pedido.
 */
@Component
public class PedidoStateFactory {
    private final Map<EstadoPedido, EstadoPedidoState> estados = new EnumMap<>(EstadoPedido.class);

    public PedidoStateFactory() {
        registrar(new PendienteState());
        registrar(new ConfirmadoState());
        registrar(new PreparandoState());
        registrar(new CaminoState());
        registrar(new EntregadoState());
        registrar(new CanceladoState());
    }

    public EstadoPedidoState crear(EstadoPedido estado) {
        EstadoPedidoState resultado = estados.get(estado);
        if (resultado == null) {
            throw new IllegalArgumentException("Estado de pedido no soportado: " + estado);
        }
        return resultado;
    }

    private void registrar(EstadoPedidoState state) {
        estados.put(state.estado(), state);
    }
}
