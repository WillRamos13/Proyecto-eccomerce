package com.fastmarket.api.service;

import com.fastmarket.api.dto.CuponDtos;
import com.fastmarket.api.dto.EstadisticasDtos;
import com.fastmarket.api.dto.PagoDtos;
import com.fastmarket.api.dto.PedidoDtos;
import com.fastmarket.api.exception.PagoDuplicadoException;
import com.fastmarket.api.model.*;
import com.fastmarket.api.pattern.behavioral.observer.PedidoEventPublisher;
import com.fastmarket.api.pattern.behavioral.observer.PedidoEvento;
import com.fastmarket.api.pattern.creational.builder.PedidoBuilder;
import com.fastmarket.api.pattern.creational.factory.PedidoStateFactory;
import com.fastmarket.api.repository.PedidoHistorialRepository;
import com.fastmarket.api.repository.PedidoRepository;
import com.fastmarket.api.repository.ProductoRepository;
import com.fastmarket.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PedidoService {
    public static final String MERCADO_PAGO = "Mercado Pago";
    public static final String PAGO_CONTRA_ENTREGA = "Pago contra entrega";

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuponService cuponService;
    private final PedidoHistorialRepository pedidoHistorialRepository;
    private final CarritoService carritoService;
    private final SystemConfigService systemConfigService;
    private final PedidoStateFactory pedidoStateFactory;
    private final PedidoEventPublisher pedidoEventPublisher;
    private final int reservaPagoMinutos;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            CuponService cuponService,
            PedidoHistorialRepository pedidoHistorialRepository,
            CarritoService carritoService,
            SystemConfigService systemConfigService,
            PedidoStateFactory pedidoStateFactory,
            PedidoEventPublisher pedidoEventPublisher,
            @Value("${app.pagos.reserva-minutos:30}") int reservaPagoMinutos
    ) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuponService = cuponService;
        this.pedidoHistorialRepository = pedidoHistorialRepository;
        this.carritoService = carritoService;
        this.systemConfigService = systemConfigService;
        this.pedidoStateFactory = pedidoStateFactory;
        this.pedidoEventPublisher = pedidoEventPublisher;
        this.reservaPagoMinutos = Math.max(5, reservaPagoMinutos);
    }

    @Transactional(readOnly = true)
    public Page<PedidoDtos.PedidoResponse> listarPaginado(AuthTokenService.TokenData actor, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "fecha"));
        if (actor.rol() == Rol.VENDEDOR) {
            return pedidoRepository.findByVendedorId(actor.usuarioId(), pageable).map(p -> DtoMapper.toPedidoResponse(p, actor.usuarioId()));
        }
        return pedidoRepository.findAll(pageable).map(DtoMapper::toPedidoResponse);
    }

    @Transactional(readOnly = true)
    public List<PedidoDtos.PedidoResponse> listar(AuthTokenService.TokenData actor) {
        if (actor.rol() == Rol.VENDEDOR) {
            return pedidoRepository.findByVendedorIdOrderByFechaDesc(actor.usuarioId()).stream()
                    .map(p -> DtoMapper.toPedidoResponse(p, actor.usuarioId())).toList();
        }
        return pedidoRepository.findAllByOrderByFechaDesc().stream().map(DtoMapper::toPedidoResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoDtos.PedidoResponse> listarPorVendedor(Long vendedorId) {
        return pedidoRepository.findByVendedorIdOrderByFechaDesc(vendedorId).stream()
                .map(p -> DtoMapper.toPedidoResponse(p, vendedorId)).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoDtos.PedidoResponse> listarPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream().map(DtoMapper::toPedidoResponse).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDtos.PedidoResponse obtener(AuthTokenService.TokenData actor, Long pedidoId) {
        return DtoMapper.toPedidoResponse(obtenerPedidoAutorizado(actor, pedidoId));
    }

    @Transactional(readOnly = true)
    public Pedido obtenerPedidoAutorizado(AuthTokenService.TokenData actor, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        validarAccesoPedido(actor, pedido);
        return pedido;
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Referencia de pedido inválida");
        return pedidoRepository.findByCodigo(codigo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<PedidoDtos.HistorialResponse> historial(AuthTokenService.TokenData actor, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        validarAccesoPedido(actor, pedido);
        return pedidoHistorialRepository.findByPedidoIdOrderByFechaAsc(pedidoId).stream()
                .map(h -> new PedidoDtos.HistorialResponse(
                        h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(),
                        h.getActor() != null ? h.getActor().getNombre() : "Sistema",
                        h.getMotivo(), h.getFecha()))
                .toList();
    }

    @Transactional
    public PedidoDtos.PedidoResponse crear(Long usuarioId, PedidoDtos.CrearPedidoRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String metodoPago = normalizarMetodoPago(request.metodoPago());

        PedidoBuilder builder = PedidoBuilder.nuevo(usuario)
                .conCodigo(generarCodigoPedido())
                .conEntrega(request.direccionEntrega(), request.referenciaEntrega(), request.horarioEntrega(), request.telefonoEntrega())
                .conMetodoPago(metodoPago);

        Map<Long, Integer> cantidadesPorProducto = agruparItemsPedido(request.items());
        if (cantidadesPorProducto.isEmpty()) throw new IllegalArgumentException("El carrito está vacío");

        List<CuponDtos.AplicarCuponItemRequest> itemsCupon = cantidadesPorProducto.entrySet().stream()
                .map(entry -> new CuponDtos.AplicarCuponItemRequest(entry.getKey(), entry.getValue()))
                .toList();

        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productoRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " ya no está disponible");
            }
            int cantidad = entry.getValue();
            int stock = producto.getStock() == null ? 0 : producto.getStock();
            if (stock < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre() + ". Stock actual: " + stock);
            }
            producto.setStock(stock - cantidad);
            productoRepository.save(producto);
            builder.agregarProducto(producto, cantidad);
        }

        BigDecimal subtotalPedido = builder.subtotalActual();
        BigDecimal costoEnvio = calcularCostoEnvio(subtotalPedido);
        BigDecimal descuento = BigDecimal.ZERO;
        CuponService.CalculoCupon calculoCupon = null;

        if (request.cuponCodigo() != null && !request.cuponCodigo().isBlank()) {
            calculoCupon = cuponService.calcularDescuento(request.cuponCodigo(), itemsCupon, usuarioId);
            descuento = calculoCupon.descuento();
            builder.conCupon(calculoCupon.cupon() != null ? calculoCupon.cupon().getCodigo() : request.cuponCodigo());
        }

        Pedido pedido = builder.conTotales(subtotalPedido, costoEnvio, descuento).build();
        pedido.setStockDescontado(true);
        pedido.setEstadoPago(EstadoPago.PENDIENTE);
        if (MERCADO_PAGO.equals(metodoPago)) {
            pedido.setEstado(EstadoPedido.PENDIENTE);
            pedido.setFechaExpiracionPago(LocalDateTime.now().plusMinutes(reservaPagoMinutos));
        } else if (PAGO_CONTRA_ENTREGA.equals(metodoPago)) {
            pedido.setEstado(EstadoPedido.CONFIRMADO);
        } else {
            pedido.setEstado(EstadoPedido.PENDIENTE);
        }

        Pedido guardado = pedidoRepository.save(pedido);
        if (descuento.compareTo(BigDecimal.ZERO) > 0 && calculoCupon != null && calculoCupon.cupon() != null) {
            cuponService.registrarUso(calculoCupon.cupon(), usuario, guardado, descuento);
            guardado.setCuponUsoActivo(true);
            guardado = pedidoRepository.save(guardado);
        }

        pedidoEventPublisher.publicar(new PedidoEvento(
                guardado, null, guardado.getEstado(), usuario,
                MERCADO_PAGO.equals(metodoPago) ? "Pedido creado; stock reservado hasta completar el pago" : "Pedido creado"
        ));

        if (!MERCADO_PAGO.equals(metodoPago)) carritoService.limpiar(usuarioId);
        return DtoMapper.toPedidoResponse(guardado);
    }

    @Transactional
    public PedidoDtos.PedidoResponse actualizarEstado(AuthTokenService.TokenData actor, Long id, EstadoPedido estado) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (actor.rol() == Rol.VENDEDOR) {
            throw new SecurityException("El vendedor no puede cambiar el estado general del pedido");
        }
        EstadoPedido anterior = pedido.getEstado();
        if (anterior == estado) return DtoMapper.toPedidoResponse(pedido);

        Usuario actorUsuario = usuarioRepository.findById(actor.usuarioId()).orElse(null);
        pedidoStateFactory.crear(anterior).validarTransicion(estado);

        if (estado == EstadoPedido.CANCELADO) {
            if (pedido.getEstadoPago() == EstadoPago.APROBADO) {
                throw new IllegalStateException("El pedido ya fue pagado. Usa la opción de reembolso para cancelarlo.");
            }
            return DtoMapper.toPedidoResponse(cancelarInterno(pedido, actorUsuario, "Pedido cancelado", EstadoPago.CANCELADO));
        }

        if ((estado == EstadoPedido.CONFIRMADO || estado == EstadoPedido.PREPARANDO)
                && !puedeAvanzarLogistica(pedido)) {
            throw new IllegalStateException("El pago debe estar aprobado antes de preparar el pedido");
        }

        pedido.setEstado(estado);
        if (estado == EstadoPedido.ENTREGADO
                && PAGO_CONTRA_ENTREGA.equals(pedido.getMetodoPago())
                && pedido.getEstadoPago() == EstadoPago.PENDIENTE) {
            pedido.setEstadoPago(EstadoPago.APROBADO);
            pedido.setMontoPagado(pedido.getTotal());
            pedido.setFechaPago(LocalDateTime.now());
            pedido.setReferenciaPago("CONTRA-ENTREGA-" + pedido.getCodigo());
        }

        Pedido guardado = pedidoRepository.save(pedido);
        pedidoEventPublisher.publicar(new PedidoEvento(guardado, anterior, estado, actorUsuario, "Cambio de estado"));
        return DtoMapper.toPedidoResponse(guardado);
    }

    @Transactional
    public void registrarPreferencia(Long pedidoId, String preferenciaId) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (pedido.getEstadoPago() != EstadoPago.PENDIENTE || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new IllegalStateException("El pedido ya no admite una nueva preferencia de pago");
        }
        pedido.setPreferenciaPagoId(limpiar(preferenciaId));
        pedidoRepository.save(pedido);
    }

    @Transactional
    public PedidoDtos.PedidoResponse confirmarPagoMercadoPago(Long pedidoId, PagoDtos.PagoVerificado pago) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (pedido.getEstadoPago() == EstadoPago.APROBADO) {
            String pagoExistente = limpiar(pedido.getPagoId());
            String pagoEntrante = limpiar(pago.id());
            if (Objects.equals(pagoExistente, pagoEntrante)) return DtoMapper.toPedidoResponse(pedido);
            throw new PagoDuplicadoException("El pedido ya tiene otro pago aprobado");
        }
        if (pedido.getEstado() == EstadoPedido.CANCELADO) throw new IllegalStateException("El pedido fue cancelado y ya no puede pagarse");
        if (!MERCADO_PAGO.equals(pedido.getMetodoPago())) throw new IllegalArgumentException("El pedido no utiliza Mercado Pago");
        if (pago.transactionAmount() == null || pago.transactionAmount().subtract(pedido.getTotal()).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalStateException("El monto aprobado no coincide con el total del pedido");
        }

        EstadoPedido anterior = pedido.getEstado();
        pedido.setEstadoPago(EstadoPago.APROBADO);
        actualizarIdentificadoresPago(pedido, pago);
        pedido.setMontoPagado(pago.transactionAmount().setScale(2, RoundingMode.HALF_UP));
        pedido.setFechaPago(pago.dateApproved() == null ? LocalDateTime.now() : pago.dateApproved());
        pedido.setFechaExpiracionPago(null);
        if (pedido.getEstado() == EstadoPedido.PENDIENTE) pedido.setEstado(EstadoPedido.CONFIRMADO);

        Pedido guardado = pedidoRepository.save(pedido);
        carritoService.limpiar(pedido.getUsuario().getId());
        pedidoEventPublisher.publicar(new PedidoEvento(
                guardado, anterior, guardado.getEstado(), null,
                "Pago de Mercado Pago verificado por el servidor"
        ));
        return DtoMapper.toPedidoResponse(guardado);
    }

    @Transactional
    public PedidoDtos.PedidoResponse registrarPagoNoAprobado(Long pedidoId, PagoDtos.PagoVerificado pago) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (pedido.getEstadoPago() == EstadoPago.APROBADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            return DtoMapper.toPedidoResponse(pedido);
        }
        actualizarIdentificadoresPago(pedido, pago);
        String status = pago.status() == null ? "" : pago.status().toLowerCase(Locale.ROOT);
        EstadoPago estadoPago = status.equals("cancelled") ? EstadoPago.CANCELADO : EstadoPago.RECHAZADO;
        return DtoMapper.toPedidoResponse(cancelarInterno(pedido, null, "Pago " + status, estadoPago));
    }

    @Transactional
    public PedidoDtos.PedidoResponse registrarReembolsoSistema(Long pedidoId, PagoDtos.PagoVerificado pago) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (pedido.getEstadoPago() == EstadoPago.REEMBOLSADO) return DtoMapper.toPedidoResponse(pedido);
        actualizarIdentificadoresPago(pedido, pago);
        return DtoMapper.toPedidoResponse(cancelarInterno(pedido, null, "Reembolso confirmado por Mercado Pago", EstadoPago.REEMBOLSADO));
    }

    @Transactional
    public PedidoDtos.PedidoResponse cancelarPagoPendiente(AuthTokenService.TokenData actor, Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        validarAccesoPedido(actor, pedido);
        if (!MERCADO_PAGO.equals(pedido.getMetodoPago())) throw new IllegalArgumentException("Solo aplica a pagos de Mercado Pago");
        if (pedido.getEstadoPago() != EstadoPago.PENDIENTE) throw new IllegalStateException("El pago ya fue procesado");
        Usuario actorUsuario = usuarioRepository.findById(actor.usuarioId()).orElse(null);
        return DtoMapper.toPedidoResponse(cancelarInterno(pedido, actorUsuario, "Pago cancelado por el cliente", EstadoPago.CANCELADO));
    }

    @Transactional
    public PedidoDtos.PedidoResponse confirmarPagoManual(AuthTokenService.TokenData actor, Long pedidoId, String referencia) {
        if (actor.rol() != Rol.ADMIN) throw new SecurityException("Solo el administrador puede confirmar pagos manuales");
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (MERCADO_PAGO.equals(pedido.getMetodoPago())) throw new IllegalArgumentException("Los pagos de Mercado Pago se verifican automáticamente");
        if (pedido.getEstado() == EstadoPedido.CANCELADO) throw new IllegalStateException("El pedido está cancelado");
        if (pedido.getEstadoPago() == EstadoPago.APROBADO) return DtoMapper.toPedidoResponse(pedido);

        EstadoPedido anterior = pedido.getEstado();
        pedido.setEstadoPago(EstadoPago.APROBADO);
        pedido.setReferenciaPago(referencia == null ? null : referencia.trim());
        pedido.setMontoPagado(pedido.getTotal());
        pedido.setFechaPago(LocalDateTime.now());
        if (pedido.getEstado() == EstadoPedido.PENDIENTE) pedido.setEstado(EstadoPedido.CONFIRMADO);
        Pedido guardado = pedidoRepository.save(pedido);
        Usuario actorUsuario = usuarioRepository.findById(actor.usuarioId()).orElse(null);
        pedidoEventPublisher.publicar(new PedidoEvento(guardado, anterior, guardado.getEstado(), actorUsuario, "Pago manual confirmado"));
        return DtoMapper.toPedidoResponse(guardado);
    }

    @Transactional
    public PedidoDtos.PedidoResponse registrarReembolso(AuthTokenService.TokenData actor, Long pedidoId) {
        if (actor.rol() != Rol.ADMIN) throw new SecurityException("Solo el administrador puede registrar reembolsos");
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (pedido.getEstadoPago() != EstadoPago.APROBADO) throw new IllegalStateException("El pedido no tiene un pago aprobado");
        Usuario actorUsuario = usuarioRepository.findById(actor.usuarioId()).orElse(null);
        return DtoMapper.toPedidoResponse(cancelarInterno(pedido, actorUsuario, "Pago reembolsado y pedido cancelado", EstadoPago.REEMBOLSADO));
    }

    @Transactional
    public int expirarReservasPendientes() {
        List<Pedido> vencidos = pedidoRepository.findByEstadoPagoAndFechaExpiracionPagoBefore(EstadoPago.PENDIENTE, LocalDateTime.now());
        int procesados = 0;
        for (Pedido pedido : vencidos) {
            if (pedido.getEstado() == EstadoPedido.CANCELADO || !MERCADO_PAGO.equals(pedido.getMetodoPago())) continue;
            cancelarInterno(pedido, null, "Reserva de pago expirada", EstadoPago.CANCELADO);
            procesados++;
        }
        return procesados;
    }

    private Pedido cancelarInterno(Pedido pedido, Usuario actor, String motivo, EstadoPago estadoPagoFinal) {
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            // La cancelación puede haberse realizado antes de recibir una confirmación tardía de reembolso.
            // Se actualiza únicamente el estado financiero, sin devolver stock ni cupón por segunda vez.
            if (estadoPagoFinal == EstadoPago.REEMBOLSADO && pedido.getEstadoPago() != EstadoPago.REEMBOLSADO) {
                pedido.setEstadoPago(EstadoPago.REEMBOLSADO);
                pedido.setFechaExpiracionPago(null);
                return pedidoRepository.save(pedido);
            }
            return pedido;
        }
        EstadoPedido anterior = pedido.getEstado();

        if (Boolean.TRUE.equals(pedido.getStockDescontado())) {
            for (PedidoItem item : pedido.getItems()) {
                if (item.getProducto() == null || item.getProducto().getId() == null) continue;
                Producto producto = productoRepository.findByIdForUpdate(item.getProducto().getId()).orElse(null);
                if (producto == null) continue;
                int stock = producto.getStock() == null ? 0 : producto.getStock();
                int cantidad = item.getCantidad() == null ? 0 : item.getCantidad();
                producto.setStock(stock + cantidad);
                productoRepository.save(producto);
            }
            pedido.setStockDescontado(false);
        }

        if (Boolean.TRUE.equals(pedido.getCuponUsoActivo())) {
            cuponService.revertirUso(pedido);
            pedido.setCuponUsoActivo(false);
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido.setEstadoPago(estadoPagoFinal);
        pedido.setFechaExpiracionPago(null);
        Pedido guardado = pedidoRepository.save(pedido);
        pedidoEventPublisher.publicar(new PedidoEvento(guardado, anterior, EstadoPedido.CANCELADO, actor, motivo));
        return guardado;
    }

    private boolean puedeAvanzarLogistica(Pedido pedido) {
        return pedido.getEstadoPago() == EstadoPago.APROBADO || PAGO_CONTRA_ENTREGA.equals(pedido.getMetodoPago());
    }

    private boolean esVentaConfirmada(Pedido pedido) {
        return pedido != null && pedido.getEstado() != EstadoPedido.CANCELADO && pedido.getEstadoPago() == EstadoPago.APROBADO;
    }

    private void validarAccesoPedido(AuthTokenService.TokenData actor, Pedido pedido) {
        if (actor.rol() == Rol.ADMIN) return;
        if (actor.rol() == Rol.CLIENTE && pedido.getUsuario().getId().equals(actor.usuarioId())) return;
        if (actor.rol() == Rol.VENDEDOR) {
            boolean pertenece = pedido.getItems().stream().anyMatch(i -> i.getVendedor() != null && i.getVendedor().getId().equals(actor.usuarioId()));
            if (pertenece) return;
        }
        throw new SecurityException("No autorizado para acceder a este pedido");
    }

    private String generarCodigoPedido() {
        return "PED-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private String normalizarMetodoPago(String metodoPago) {
        if (metodoPago == null) throw new IllegalArgumentException("El método de pago es obligatorio");
        String limpio = metodoPago.trim();
        Set<String> permitidos = Set.of(PAGO_CONTRA_ENTREGA, "Yape / Plin", "Transferencia bancaria", MERCADO_PAGO);
        if (!permitidos.contains(limpio)) throw new IllegalArgumentException("Método de pago no permitido");
        return limpio;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void actualizarIdentificadoresPago(Pedido pedido, PagoDtos.PagoVerificado pago) {
        String pagoId = limpiar(pago.id());
        String preferenciaId = limpiar(pago.preferenceId());
        String referencia = limpiar(pago.externalReference());
        if (pagoId != null) pedido.setPagoId(pagoId);
        if (preferenciaId != null) pedido.setPreferenciaPagoId(preferenciaId);
        if (referencia != null) pedido.setReferenciaPago(referencia);
    }

    private Map<Long, Integer> agruparItemsPedido(List<PedidoDtos.ItemRequest> items) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        if (items == null) return cantidades;
        for (PedidoDtos.ItemRequest item : items) {
            if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) continue;
            try {
                cantidades.merge(item.productoId(), item.cantidad(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("La cantidad solicitada es demasiado grande");
            }
        }
        return cantidades;
    }

    private BigDecimal calcularCostoEnvio(BigDecimal subtotalPedido) {
        BigDecimal costoConfigurado = systemConfigService.obtenerDecimal(SystemConfigService.COSTO_ENVIO, new BigDecimal("8.00"));
        if (costoConfigurado.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (subtotalPedido != null && subtotalPedido.compareTo(new BigDecimal("250.00")) >= 0) return BigDecimal.ZERO;
        return costoConfigurado;
    }

    @Transactional(readOnly = true)
    public EstadisticasDtos.EstadisticasVendedorResponse obtenerEstadisticasVendedor(AuthTokenService.TokenData actor, Long vendedorId, int diasGrafico) {
        if (actor.rol() == Rol.VENDEDOR && !actor.usuarioId().equals(vendedorId)) throw new SecurityException("No autorizado para ver estas estadísticas");
        if (actor.rol() == Rol.CLIENTE) throw new SecurityException("No autorizado para ver estas estadísticas");

        int rango = diasGrafico <= 0 ? 14 : Math.min(diasGrafico, 90);
        List<Pedido> pedidos = pedidoRepository.findByVendedorIdOrderByFechaDesc(vendedorId);
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.minusDays(6);
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        BigDecimal ventasHoy = BigDecimal.ZERO, ventasSemana = BigDecimal.ZERO, ventasMes = BigDecimal.ZERO, ventasTotal = BigDecimal.ZERO;
        long pedidosHoy = 0, pedidosSemana = 0, pedidosMes = 0, pedidosValidos = 0, unidadesVendidas = 0;

        Map<LocalDate, BigDecimal> ventasPorDiaMap = new LinkedHashMap<>();
        Map<LocalDate, Long> pedidosPorDiaMap = new LinkedHashMap<>();
        for (int i = rango - 1; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            ventasPorDiaMap.put(dia, BigDecimal.ZERO);
            pedidosPorDiaMap.put(dia, 0L);
        }

        Map<Long, String> nombreProducto = new LinkedHashMap<>();
        Map<Long, Long> unidadesPorProducto = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalPorProducto = new LinkedHashMap<>();
        Map<EstadoPedido, Long> cantidadPorEstado = new EnumMap<>(EstadoPedido.class);
        Map<EstadoPedido, BigDecimal> totalPorEstado = new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido estado : EstadoPedido.values()) {
            cantidadPorEstado.put(estado, 0L);
            totalPorEstado.put(estado, BigDecimal.ZERO);
        }

        for (Pedido pedido : pedidos) {
            List<PedidoItem> itemsVendedor = pedido.getItems().stream()
                    .filter(i -> i.getVendedor() != null && i.getVendedor().getId().equals(vendedorId)).toList();
            if (itemsVendedor.isEmpty()) continue;

            BigDecimal totalPedidoVendedor = itemsVendedor.stream()
                    .map(i -> i.getSubtotal() == null ? BigDecimal.ZERO : i.getSubtotal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long unidadesPedido = itemsVendedor.stream().mapToLong(i -> i.getCantidad() == null ? 0 : i.getCantidad()).sum();
            LocalDate fechaPedido = pedido.getFecha() != null ? pedido.getFecha().toLocalDate() : hoy;
            EstadoPedido estado = pedido.getEstado() != null ? pedido.getEstado() : EstadoPedido.PENDIENTE;

            cantidadPorEstado.merge(estado, 1L, Long::sum);
            if (!esVentaConfirmada(pedido)) continue;
            totalPorEstado.merge(estado, totalPedidoVendedor, BigDecimal::add);

            pedidosValidos++;
            ventasTotal = ventasTotal.add(totalPedidoVendedor);
            unidadesVendidas += unidadesPedido;
            if (!fechaPedido.isBefore(hoy)) { ventasHoy = ventasHoy.add(totalPedidoVendedor); pedidosHoy++; }
            if (!fechaPedido.isBefore(inicioSemana)) { ventasSemana = ventasSemana.add(totalPedidoVendedor); pedidosSemana++; }
            if (!fechaPedido.isBefore(inicioMes)) { ventasMes = ventasMes.add(totalPedidoVendedor); pedidosMes++; }
            if (ventasPorDiaMap.containsKey(fechaPedido)) {
                ventasPorDiaMap.merge(fechaPedido, totalPedidoVendedor, BigDecimal::add);
                pedidosPorDiaMap.merge(fechaPedido, 1L, Long::sum);
            }

            for (PedidoItem item : itemsVendedor) {
                Long productoId = item.getProducto() != null ? item.getProducto().getId() : null;
                if (productoId == null) continue;
                nombreProducto.putIfAbsent(productoId, item.getProductoNombre());
                long cantidad = item.getCantidad() == null ? 0 : item.getCantidad();
                BigDecimal subtotal = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();
                unidadesPorProducto.merge(productoId, cantidad, Long::sum);
                totalPorProducto.merge(productoId, subtotal, BigDecimal::add);
            }
        }

        BigDecimal ticketPromedio = pedidosValidos == 0 ? BigDecimal.ZERO
                : ventasTotal.divide(BigDecimal.valueOf(pedidosValidos), 2, RoundingMode.HALF_UP);
        EstadisticasDtos.ResumenVentas resumen = new EstadisticasDtos.ResumenVentas(
                escala(ventasHoy), escala(ventasSemana), escala(ventasMes), escala(ventasTotal),
                pedidosHoy, pedidosSemana, pedidosMes, pedidosValidos, unidadesVendidas, escala(ticketPromedio));

        List<EstadisticasDtos.VentaPorDia> ventasPorDia = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entrada : ventasPorDiaMap.entrySet()) {
            ventasPorDia.add(new EstadisticasDtos.VentaPorDia(
                    entrada.getKey(), escala(entrada.getValue()), pedidosPorDiaMap.getOrDefault(entrada.getKey(), 0L)));
        }

        List<EstadisticasDtos.ProductoTop> topProductos = unidadesPorProducto.entrySet().stream()
                .map(e -> new EstadisticasDtos.ProductoTop(e.getKey(), nombreProducto.getOrDefault(e.getKey(), "Producto"),
                        e.getValue(), escala(totalPorProducto.getOrDefault(e.getKey(), BigDecimal.ZERO))))
                .sorted(Comparator.comparing(EstadisticasDtos.ProductoTop::totalVentas).reversed()).limit(5).toList();

        List<EstadisticasDtos.VentaPorEstado> porEstado = new ArrayList<>();
        for (EstadoPedido estado : EstadoPedido.values()) {
            long cantidad = cantidadPorEstado.getOrDefault(estado, 0L);
            if (cantidad > 0) porEstado.add(new EstadisticasDtos.VentaPorEstado(
                    estado, cantidad, escala(totalPorEstado.getOrDefault(estado, BigDecimal.ZERO))));
        }
        return new EstadisticasDtos.EstadisticasVendedorResponse(resumen, ventasPorDia, topProductos, porEstado);
    }

    private BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
