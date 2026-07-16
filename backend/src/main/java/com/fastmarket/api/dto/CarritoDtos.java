package com.fastmarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CarritoDtos {
    public record CarritoItemRequest(Long productoId, Integer cantidad) {}
    public record SincronizarCarritoRequest(String cuponCodigo, List<CarritoItemRequest> items) {}
    public record CarritoItemResponse(Long productoId, String nombre, String imagen, BigDecimal precio, Integer cantidad, BigDecimal subtotal, Integer stockDisponible) {}
    public record CarritoResponse(Long usuarioId, String cuponCodigo, BigDecimal subtotal, BigDecimal descuento, BigDecimal total, LocalDateTime actualizadoEn, List<CarritoItemResponse> items) {}
}
