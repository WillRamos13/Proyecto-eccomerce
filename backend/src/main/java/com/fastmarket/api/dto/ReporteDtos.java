package com.fastmarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReporteDtos {
    public record Periodo(
            LocalDate desde,
            LocalDate hasta,
            LocalDateTime generadoEn
    ) {}

    public record Resumen(
            long usuariosTotales,
            long usuariosNuevos,
            long usuariosActivos,
            long sesionesExitosas,
            long sesionesFallidas,
            long usuariosUnicosConSesion,
            long pedidosRegistrados,
            long ventasValidas,
            long pedidosEntregados,
            long pedidosCancelados,
            BigDecimal ingresos,
            BigDecimal ticketPromedio,
            BigDecimal descuentosOtorgados,
            BigDecimal ingresosEnvio,
            long unidadesVendidas,
            long clientesCompradores,
            long clientesRecurrentes,
            double tasaEntrega,
            double tasaCancelacion,
            long reclamos,
            long reclamosPendientes,
            long reclamosResueltos,
            Double horasPromedioResolucion,
            long productosActivos,
            long productosStockBajo,
            long productosAgotados,
            long stockTotal
    ) {}

    public record ActividadDia(
            LocalDate fecha,
            long usuariosNuevos,
            long sesionesExitosas,
            long sesionesFallidas,
            long pedidos,
            BigDecimal ventas,
            long reclamos
    ) {}

    public record DistribucionCantidad(String nombre, long cantidad) {}
    public record DistribucionMonto(String nombre, long cantidad, BigDecimal total) {}

    public record ProductoTop(
            Long productoId,
            String nombre,
            long unidades,
            BigDecimal ventas
    ) {}

    public record StockCritico(
            Long productoId,
            String nombre,
            String categoria,
            int stock,
            String vendedor
    ) {}

    public record ReporteAdminResponse(
            Periodo periodo,
            Resumen resumen,
            List<ActividadDia> actividadDiaria,
            List<DistribucionCantidad> usuariosPorRol,
            List<DistribucionCantidad> pedidosPorEstado,
            List<DistribucionMonto> ventasPorMetodoPago,
            List<DistribucionCantidad> reclamosPorEstado,
            List<DistribucionCantidad> reclamosPorTipo,
            List<DistribucionCantidad> productosPorCategoria,
            List<ProductoTop> productosMasVendidos,
            List<StockCritico> stockCritico,
            List<ReclamoDtos.ReclamoResponse> reclamosRecientes
    ) {}
}
