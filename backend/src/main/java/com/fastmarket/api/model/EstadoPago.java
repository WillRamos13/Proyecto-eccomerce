package com.fastmarket.api.model;

/** Estado financiero independiente del avance logístico del pedido. */
public enum EstadoPago {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    CANCELADO,
    REEMBOLSADO
}
