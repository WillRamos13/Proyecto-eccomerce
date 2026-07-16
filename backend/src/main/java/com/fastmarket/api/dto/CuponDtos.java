package com.fastmarket.api.dto;

import com.fastmarket.api.model.TipoCupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CuponDtos {
    public record CuponRequest(
            String codigo,
            String descripcion,
            TipoCupon tipo,
            Long vendedorId,
            String categoriaObjetivo,
            Long productoObjetivoId,
            BigDecimal porcentaje,
            BigDecimal montoFijo,
            BigDecimal montoMinimo,
            Integer usosMaximos,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Boolean activo
    ) {}

    public record CuponResponse(
            Long id,
            String codigo,
            String descripcion,
            TipoCupon tipo,
            Long vendedorId,
            String vendedorNombre,
            String categoriaObjetivo,
            Long productoObjetivoId,
            String productoObjetivoNombre,
            BigDecimal porcentaje,
            BigDecimal montoFijo,
            BigDecimal montoMinimo,
            Integer usosMaximos,
            Integer usosActuales,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Boolean activo
    ) {}

    public record AplicarCuponItemRequest(Long productoId, Integer cantidad) {}

    public record AplicarCuponRequest(String codigo, List<AplicarCuponItemRequest> items) {}

    public record CuponUsoResponse(
            Long id,
            Long cuponId,
            String codigo,
            Long usuarioId,
            String usuarioNombre,
            Long pedidoId,
            String pedidoCodigo,
            BigDecimal descuentoAplicado,
            LocalDateTime fecha
    ) {}

    public record AplicarCuponResponse(
            String codigo,
            String descripcion,
            BigDecimal subtotalAplicable,
            BigDecimal descuento,
            String mensaje
    ) {}
}
