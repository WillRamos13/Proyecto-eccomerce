package com.fastmarket.api.pattern.behavioral.observer;

import com.fastmarket.api.service.CorreoService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class PedidoNotificacionObserver implements PedidoObserver {
    private final CorreoService correoService;

    public PedidoNotificacionObserver(CorreoService correoService) {
        this.correoService = correoService;
    }

    @Override
    public void actualizar(PedidoEvento evento) {
        correoService.enviarActualizacionPedido(
                evento.pedido(),
                evento.estadoAnterior(),
                evento.estadoNuevo()
        );
    }
}
