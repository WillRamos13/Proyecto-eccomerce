package com.fastmarket.api.exception;

/**
 * Señala que Mercado Pago aprobó una segunda transacción para un pedido que ya
 * tenía otro pago aprobado. La capa de integración debe reembolsar el pago nuevo.
 */
public class PagoDuplicadoException extends IllegalStateException {
    public PagoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
