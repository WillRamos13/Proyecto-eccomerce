package com.fastmarket.api.service;

import com.fastmarket.api.dto.ReclamoDtos;
import com.fastmarket.api.dto.ReporteDtos;
import com.fastmarket.api.model.*;
import com.fastmarket.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReporteService {
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final LoginEventoRepository loginEventoRepository;
    private final ReclamoRepository reclamoRepository;
    private final ReclamoService reclamoService;

    public ReporteService(
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            LoginEventoRepository loginEventoRepository,
            ReclamoRepository reclamoRepository,
            ReclamoService reclamoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.loginEventoRepository = loginEventoRepository;
        this.reclamoRepository = reclamoRepository;
        this.reclamoService = reclamoService;
    }

    @Transactional(readOnly = true)
    public ReporteDtos.ReporteAdminResponse generar(LocalDate desde, LocalDate hasta) {
        LocalDate fechaHasta = hasta == null ? LocalDate.now() : hasta;
        LocalDate fechaDesde = desde == null ? fechaHasta.minusDays(29) : desde;
        if (fechaDesde.isAfter(fechaHasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        if (ChronoUnit.DAYS.between(fechaDesde, fechaHasta) > 366) throw new IllegalArgumentException("El rango máximo del reporte es de 367 días");

        LocalDateTime inicio = fechaDesde.atStartOfDay();
        LocalDateTime finExclusivo = fechaHasta.plusDays(1).atStartOfDay();

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Pedido> pedidos = pedidoRepository.findByFechaBetweenOrderByFechaAsc(inicio, finExclusivo);
        List<Producto> productos = productoRepository.findAll();
        List<LoginEvento> accesos = loginEventoRepository.findByFechaBetweenOrderByFechaAsc(inicio, finExclusivo);
        List<Reclamo> reclamos = reclamoRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(inicio, finExclusivo);

        List<Usuario> usuariosNuevos = usuarios.stream()
                .filter(u -> u.getCreadoEn() != null && !u.getCreadoEn().isBefore(inicio) && u.getCreadoEn().isBefore(finExclusivo))
                .toList();
        List<Pedido> ventasValidas = pedidos.stream().filter(this::esVentaConfirmada).toList();

        BigDecimal ingresos = sumarPedidos(ventasValidas, "total");
        BigDecimal descuentos = sumarPedidos(ventasValidas, "descuento");
        BigDecimal envios = sumarPedidos(ventasValidas, "envio");
        long unidades = ventasValidas.stream().flatMap(p -> p.getItems().stream()).mapToLong(i -> nvl(i.getCantidad())).sum();
        long entregados = pedidos.stream().filter(p -> p.getEstado() == EstadoPedido.ENTREGADO).count();
        long cancelados = pedidos.stream().filter(p -> p.getEstado() == EstadoPedido.CANCELADO).count();
        long sesionesExitosas = accesos.stream().filter(a -> Boolean.TRUE.equals(a.getExitoso())).count();
        long sesionesFallidas = accesos.size() - sesionesExitosas;
        long usuariosUnicosSesion = accesos.stream().filter(a -> Boolean.TRUE.equals(a.getExitoso())).map(LoginEvento::getCorreo).filter(Objects::nonNull).distinct().count();
        long usuariosActivos = usuarios.stream().filter(u -> u.getEstado() == EstadoUsuario.ACTIVO).count();

        Map<Long, Long> comprasPorCliente = new HashMap<>();
        ventasValidas.forEach(p -> comprasPorCliente.merge(p.getUsuario().getId(), 1L, Long::sum));
        long clientesRecurrentes = comprasPorCliente.values().stream().filter(v -> v > 1).count();

        long reclamosPendientes = reclamos.stream().filter(r -> r.getEstado() == EstadoReclamo.ABIERTO || r.getEstado() == EstadoReclamo.EN_REVISION).count();
        long reclamosResueltos = reclamos.stream().filter(r -> r.getEstado() == EstadoReclamo.RESUELTO || r.getEstado() == EstadoReclamo.CERRADO).count();
        Double horasPromedio = promedioHorasResolucion(reclamos);

        List<Producto> activos = productos.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).toList();
        long agotados = activos.stream().filter(p -> nvl(p.getStock()) == 0).count();
        long stockBajo = activos.stream().filter(p -> nvl(p.getStock()) > 0 && nvl(p.getStock()) <= 5).count();
        long stockTotal = activos.stream().mapToLong(p -> nvl(p.getStock())).sum();

        BigDecimal ticket = ventasValidas.isEmpty()
                ? BigDecimal.ZERO
                : ingresos.divide(BigDecimal.valueOf(ventasValidas.size()), 2, RoundingMode.HALF_UP);

        ReporteDtos.Resumen resumen = new ReporteDtos.Resumen(
                usuarios.size(), usuariosNuevos.size(), usuariosActivos,
                sesionesExitosas, sesionesFallidas, usuariosUnicosSesion,
                pedidos.size(), ventasValidas.size(), entregados, cancelados,
                dinero(ingresos), dinero(ticket), dinero(descuentos), dinero(envios), unidades,
                comprasPorCliente.size(), clientesRecurrentes,
                porcentaje(entregados, ventasValidas.size()), porcentaje(cancelados, pedidos.size()),
                reclamos.size(), reclamosPendientes, reclamosResueltos, horasPromedio,
                activos.size(), stockBajo, agotados, stockTotal
        );

        return new ReporteDtos.ReporteAdminResponse(
                new ReporteDtos.Periodo(fechaDesde, fechaHasta, LocalDateTime.now()),
                resumen,
                actividadDiaria(fechaDesde, fechaHasta, usuariosNuevos, accesos, pedidos, reclamos),
                distribucionUsuarios(usuarios),
                distribucionPedidos(pedidos),
                distribucionPagos(ventasValidas),
                distribucionReclamosEstado(reclamos),
                distribucionReclamosTipo(reclamos),
                distribucionCategorias(activos),
                topProductos(ventasValidas),
                stockCritico(activos),
                reclamos.stream().limit(25).map(reclamoService::toResponse).toList()
        );
    }

    private List<ReporteDtos.ActividadDia> actividadDiaria(LocalDate desde, LocalDate hasta, List<Usuario> usuarios, List<LoginEvento> accesos, List<Pedido> pedidos, List<Reclamo> reclamos) {
        Map<LocalDate, ActividadMutable> mapa = new LinkedHashMap<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) mapa.put(d, new ActividadMutable());
        usuarios.forEach(u -> mapa.get(u.getCreadoEn().toLocalDate()).usuariosNuevos++);
        accesos.forEach(a -> {
            ActividadMutable m = mapa.get(a.getFecha().toLocalDate());
            if (Boolean.TRUE.equals(a.getExitoso())) m.sesionesExitosas++; else m.sesionesFallidas++;
        });
        pedidos.forEach(p -> {
            ActividadMutable m = mapa.get(p.getFecha().toLocalDate());
            m.pedidos++;
            if (esVentaConfirmada(p)) m.ventas = m.ventas.add(nvl(p.getTotal()));
        });
        reclamos.forEach(r -> mapa.get(r.getFechaCreacion().toLocalDate()).reclamos++);
        return mapa.entrySet().stream().map(e -> new ReporteDtos.ActividadDia(
                e.getKey(), e.getValue().usuariosNuevos, e.getValue().sesionesExitosas,
                e.getValue().sesionesFallidas, e.getValue().pedidos, dinero(e.getValue().ventas), e.getValue().reclamos
        )).toList();
    }

    private List<ReporteDtos.DistribucionCantidad> distribucionUsuarios(List<Usuario> usuarios) {
        return Arrays.stream(Rol.values()).map(rol -> new ReporteDtos.DistribucionCantidad(
                nombreRol(rol), usuarios.stream().filter(u -> u.getRol() == rol).count()
        )).toList();
    }

    private List<ReporteDtos.DistribucionCantidad> distribucionPedidos(List<Pedido> pedidos) {
        return Arrays.stream(EstadoPedido.values()).map(estado -> new ReporteDtos.DistribucionCantidad(
                nombreEnum(estado.name()), pedidos.stream().filter(p -> p.getEstado() == estado).count()
        )).toList();
    }

    private List<ReporteDtos.DistribucionMonto> distribucionPagos(List<Pedido> pedidos) {
        Map<String, long[]> cantidades = new TreeMap<>();
        Map<String, BigDecimal> totales = new TreeMap<>();
        for (Pedido p : pedidos) {
            String metodo = p.getMetodoPago() == null || p.getMetodoPago().isBlank() ? "No especificado" : p.getMetodoPago().trim();
            cantidades.computeIfAbsent(metodo, k -> new long[1])[0]++;
            totales.merge(metodo, nvl(p.getTotal()), BigDecimal::add);
        }
        return cantidades.keySet().stream().map(k -> new ReporteDtos.DistribucionMonto(k, cantidades.get(k)[0], dinero(totales.get(k)))).toList();
    }

    private List<ReporteDtos.DistribucionCantidad> distribucionReclamosEstado(List<Reclamo> reclamos) {
        return Arrays.stream(EstadoReclamo.values()).map(estado -> new ReporteDtos.DistribucionCantidad(
                nombreEnum(estado.name()), reclamos.stream().filter(r -> r.getEstado() == estado).count()
        )).toList();
    }

    private List<ReporteDtos.DistribucionCantidad> distribucionReclamosTipo(List<Reclamo> reclamos) {
        return Arrays.stream(TipoReclamo.values()).map(tipo -> new ReporteDtos.DistribucionCantidad(
                nombreEnum(tipo.name()), reclamos.stream().filter(r -> r.getTipo() == tipo).count()
        )).toList();
    }

    private List<ReporteDtos.DistribucionCantidad> distribucionCategorias(List<Producto> productos) {
        Map<String, Long> mapa = new TreeMap<>();
        productos.forEach(p -> mapa.merge(capitalizar(p.getCategoria()), 1L, Long::sum));
        return mapa.entrySet().stream().map(e -> new ReporteDtos.DistribucionCantidad(e.getKey(), e.getValue())).toList();
    }

    private List<ReporteDtos.ProductoTop> topProductos(List<Pedido> pedidos) {
        Map<Long, ProductoAcumulado> mapa = new HashMap<>();
        pedidos.stream().flatMap(p -> p.getItems().stream()).forEach(item -> {
            Long id = item.getProducto() != null ? item.getProducto().getId() : null;
            if (id == null) return;
            ProductoAcumulado a = mapa.computeIfAbsent(id, k -> new ProductoAcumulado(item.getProductoNombre()));
            a.unidades += nvl(item.getCantidad());
            a.ventas = a.ventas.add(nvl(item.getSubtotal()));
        });
        return mapa.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().unidades, a.getValue().unidades))
                .limit(10)
                .map(e -> new ReporteDtos.ProductoTop(e.getKey(), e.getValue().nombre, e.getValue().unidades, dinero(e.getValue().ventas)))
                .toList();
    }

    private List<ReporteDtos.StockCritico> stockCritico(List<Producto> productos) {
        return productos.stream()
                .filter(p -> nvl(p.getStock()) <= 5)
                .sorted(Comparator.comparingInt(p -> nvl(p.getStock())))
                .limit(20)
                .map(p -> new ReporteDtos.StockCritico(
                        p.getId(), p.getNombre(), capitalizar(p.getCategoria()), nvl(p.getStock()),
                        p.getVendedor() != null ? p.getVendedor().getNombre() : "Tienda"
                )).toList();
    }

    private boolean esVentaConfirmada(Pedido pedido) {
        return pedido != null
                && pedido.getEstado() != EstadoPedido.CANCELADO
                && pedido.getEstadoPago() == EstadoPago.APROBADO;
    }

    private BigDecimal sumarPedidos(List<Pedido> pedidos, String campo) {
        BigDecimal total = BigDecimal.ZERO;
        for (Pedido p : pedidos) {
            total = total.add(switch (campo) {
                case "descuento" -> nvl(p.getDescuento());
                case "envio" -> nvl(p.getCostoEnvio());
                default -> nvl(p.getTotal());
            });
        }
        return total;
    }

    private Double promedioHorasResolucion(List<Reclamo> reclamos) {
        List<Double> horas = reclamos.stream()
                .filter(r -> r.getFechaResolucion() != null)
                .map(r -> Duration.between(r.getFechaCreacion(), r.getFechaResolucion()).toMinutes() / 60.0)
                .toList();
        if (horas.isEmpty()) return null;
        double promedio = horas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return Math.round(promedio * 10.0) / 10.0;
    }

    private double porcentaje(long parte, long total) {
        if (total <= 0) return 0;
        return Math.round((parte * 10000.0 / total)) / 100.0;
    }

    private BigDecimal dinero(BigDecimal valor) { return nvl(valor).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal nvl(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
    private int nvl(Integer valor) { return valor == null ? 0 : valor; }
    private String capitalizar(String valor) {
        if (valor == null || valor.isBlank()) return "Sin categoría";
        String limpio = valor.trim().replace('_', ' ');
        return limpio.substring(0, 1).toUpperCase() + limpio.substring(1).toLowerCase();
    }
    private String nombreRol(Rol rol) { return rol == Rol.ADMIN ? "Administradores" : rol == Rol.VENDEDOR ? "Vendedores" : "Clientes"; }
    private String nombreEnum(String valor) { return capitalizar(valor); }

    private static class ActividadMutable {
        long usuariosNuevos;
        long sesionesExitosas;
        long sesionesFallidas;
        long pedidos;
        BigDecimal ventas = BigDecimal.ZERO;
        long reclamos;
    }

    private static class ProductoAcumulado {
        final String nombre;
        long unidades;
        BigDecimal ventas = BigDecimal.ZERO;
        ProductoAcumulado(String nombre) { this.nombre = nombre == null ? "Producto" : nombre; }
    }
}
