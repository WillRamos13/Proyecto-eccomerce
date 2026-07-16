package com.fastmarket.api.pattern.behavioral.observer;

/** Patrón Observer: contrato pequeño para reaccionar a eventos del pedido. */
public interface PedidoObserver {
    void actualizar(PedidoEvento evento);
}
