package com.fastmarket.api.pattern.behavioral.observer;

import com.fastmarket.api.model.PedidoHistorial;
import com.fastmarket.api.repository.PedidoHistorialRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class PedidoHistorialObserver implements PedidoObserver {
    private final PedidoHistorialRepository pedidoHistorialRepository;

    public PedidoHistorialObserver(PedidoHistorialRepository pedidoHistorialRepository) {
        this.pedidoHistorialRepository = pedidoHistorialRepository;
    }

    @Override
    public void actualizar(PedidoEvento evento) {
        PedidoHistorial historial = new PedidoHistorial();
        historial.setPedido(evento.pedido());
        historial.setEstadoAnterior(evento.estadoAnterior());
        historial.setEstadoNuevo(evento.estadoNuevo());
        historial.setActor(evento.actor());
        historial.setMotivo(evento.motivo());
        pedidoHistorialRepository.save(historial);
    }
}
