package com.fastmarket.api.dto;

import com.fastmarket.api.model.EstadoPago;
import com.fastmarket.api.model.EstadoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PedidoDtos {
    public record ItemRequest(
            @NotNull Long productoId,
            @NotNull @Positive @Max(1000) Integer cantidad
    ) {}

    public record CrearPedidoRequest(
            @NotBlank @Size(max = 500) String direccionEntrega,
            @Size(max = 500) String referenciaEntrega,
            @NotBlank @Size(max = 80) String horarioEntrega,
            @NotBlank
            @Pattern(regexp = "Pago contra entrega|Yape / Plin|Transferencia bancaria|Mercado Pago", message = "método de pago no permitido")
            String metodoPago,
            @NotBlank @Size(max = 40)
            @Pattern(regexp = "[+0-9 ()-]{6,40}", message = "teléfono inválido")
            String telefonoEntrega,
            @Size(max = 40) String cuponCodigo,
            @NotEmpty @Size(max = 100) List<@Valid @NotNull ItemRequest> items
    ) {}

    public record ItemResponse(
            Long productoId,
            String productoNombre,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            String imagen
    ) {}

    public record HistorialResponse(
            Long id,
            EstadoPedido estadoAnterior,
            EstadoPedido estadoNuevo,
            String actorNombre,
            String motivo,
            LocalDateTime fecha
    ) {}

    public record PedidoResponse(
            Long id,
            String codigo,
            Long usuarioId,
            String usuarioNombre,
            BigDecimal subtotal,
            BigDecimal costoEnvio,
            BigDecimal total,
            BigDecimal descuento,
            String cuponCodigo,
            EstadoPedido estado,
            EstadoPago estadoPago,
            String pagoId,
            String preferenciaPagoId,
            String referenciaPago,
            BigDecimal montoPagado,
            LocalDateTime fechaPago,
            LocalDateTime fechaExpiracionPago,
            String direccionEntrega,
            String referenciaEntrega,
            String horarioEntrega,
            String metodoPago,
            String telefonoEntrega,
            LocalDateTime fecha,
            List<ItemResponse> items
    ) {}
}
