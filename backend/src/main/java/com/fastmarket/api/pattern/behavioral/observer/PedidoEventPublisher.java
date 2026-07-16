package com.fastmarket.api.pattern.behavioral.observer;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PedidoEventPublisher {
    private final List<PedidoObserver> observadores;

    public PedidoEventPublisher(List<PedidoObserver> observadores) {
        this.observadores = List.copyOf(observadores);
    }

    public void publicar(PedidoEvento evento) {
        for (PedidoObserver observador : observadores) {
            observador.actualizar(evento);
        }
    }
}
