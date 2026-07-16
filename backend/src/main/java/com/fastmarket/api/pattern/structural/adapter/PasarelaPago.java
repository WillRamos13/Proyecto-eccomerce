package com.fastmarket.api.pattern.structural.adapter;

import com.fastmarket.api.dto.PagoDtos;

/** Puerto estable del sistema para cualquier proveedor de pagos externo. */
public interface PasarelaPago {
    PagoDtos.PreferenciaCreada crearPreferencia(PagoDtos.PreferenciaPago solicitud);
    PagoDtos.PagoVerificado consultarPago(String paymentId);
    void reembolsar(String paymentId);
    String nombre();
}
