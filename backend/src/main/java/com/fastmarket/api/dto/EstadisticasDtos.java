package com.fastmarket.api.dto;

import com.fastmarket.api.model.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EstadisticasDtos {

    public record ResumenVentas(
            BigDecimal ventasHoy,
            BigDecimal ventasSemana,
            BigDecimal ventasMes,
            BigDecimal ventasTotal,
            long pedidosHoy,
            long pedidosSemana,
            long pedidosMes,
            long pedidosTotal,
            long unidadesVendidas,
            BigDecimal ticketPromedio
    ) {}

    public record VentaPorDia(
            LocalDate fecha,
            BigDecimal total,
            long pedidos
    ) {}

    public record ProductoTop(
            Long productoId,
            String nombre,
            long unidadesVendidas,
            BigDecimal totalVentas
    ) {}

    public record VentaPorEstado(
            EstadoPedido estado,
            long cantidad,
            BigDecimal total
    ) {}

    public record EstadisticasVendedorResponse(
            ResumenVentas resumen,
            List<VentaPorDia> ventasPorDia,
            List<ProductoTop> topProductos,
            List<VentaPorEstado> porEstado
    ) {}
}