package com.fastmarket.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PagoDtos {
    /** Solicitud interna, construida únicamente con datos persistidos del pedido. */
    public record PreferenciaPago(
            Long pedidoId,
            String codigoPedido,
            String descripcion,
            BigDecimal monto,
            String correoPagador,
            String successUrl,
            String failureUrl,
            String pendingUrl,
            String notificationUrl,
            LocalDateTime fechaExpiracion
    ) {}

    public record PreferenciaCreada(
            String proveedor,
            String id,
            String status,
            String initPoint,
            String sandboxInitPoint
    ) {}

    public record PagoVerificado(
            String id,
            String status,
            String externalReference,
            BigDecimal transactionAmount,
            String preferenceId,
            LocalDateTime dateApproved
    ) {}

    public record PagoManualRequest(
            @NotBlank @Size(max = 160) String referencia
    ) {}
}
