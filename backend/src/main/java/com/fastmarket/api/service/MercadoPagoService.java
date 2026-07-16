package com.fastmarket.api.service;

import com.fastmarket.api.dto.PagoDtos;
import com.fastmarket.api.dto.PedidoDtos;
import com.fastmarket.api.exception.PagoDuplicadoException;
import com.fastmarket.api.model.EstadoPago;
import com.fastmarket.api.model.EstadoPedido;
import com.fastmarket.api.model.Pedido;
import com.fastmarket.api.model.Rol;
import com.fastmarket.api.pattern.structural.adapter.PasarelaPago;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

/** Servicio de aplicación que depende de la abstracción de una pasarela de pago. */
@Service
public class MercadoPagoService {
    private final PasarelaPago pasarelaPago;
    private final PedidoService pedidoService;
    private final String frontendBaseUrl;
    private final String backendPublicUrl;

    public MercadoPagoService(
            PasarelaPago pasarelaPago,
            PedidoService pedidoService,
            @Value("${app.frontend.base-url:http://localhost:5500}") String frontendBaseUrl,
            @Value("${app.backend.public-url:}") String backendPublicUrl
    ) {
        this.pasarelaPago = pasarelaPago;
        this.pedidoService = pedidoService;
        this.frontendBaseUrl = sinBarraFinal(frontendBaseUrl);
        this.backendPublicUrl = sinBarraFinal(backendPublicUrl);
    }

    public PagoDtos.PreferenciaCreada crearPreferencia(AuthTokenService.TokenData actor, Long pedidoId) {
        Pedido pedido = pedidoService.obtenerPedidoAutorizado(actor, pedidoId);
        validarPedidoParaPago(pedido);

        String parametro = "pedidoId=" + pedido.getId() + "&pedido=" + pedido.getCodigo();
        PagoDtos.PreferenciaPago solicitud = new PagoDtos.PreferenciaPago(
                pedido.getId(), pedido.getCodigo(), "Pedido FastMarket " + pedido.getCodigo(), pedido.getTotal(),
                pedido.getUsuario().getCorreo(),
                frontendBaseUrl + "/pedidos.html?" + parametro + "&status=approved",
                frontendBaseUrl + "/mercado-pago.html?pedidoId=" + pedido.getId() + "&status=failure",
                frontendBaseUrl + "/mercado-pago.html?pedidoId=" + pedido.getId() + "&status=pending",
                backendPublicUrl.isBlank() ? null : backendPublicUrl + "/api/pagos/webhook",
                pedido.getFechaExpiracionPago()
        );
        PagoDtos.PreferenciaCreada preferencia = pasarelaPago.crearPreferencia(solicitud);
        if (preferencia.id() == null || (vacio(preferencia.initPoint()) && vacio(preferencia.sandboxInitPoint()))) {
            throw new IllegalStateException("Mercado Pago no devolvió una preferencia válida");
        }
        pedidoService.registrarPreferencia(pedidoId, preferencia.id());
        return preferencia;
    }

    public PedidoDtos.PedidoResponse confirmarRetorno(AuthTokenService.TokenData actor, Long pedidoId, String paymentId) {
        Pedido pedido = pedidoService.obtenerPedidoAutorizado(actor, pedidoId);
        PagoDtos.PagoVerificado pago = pasarelaPago.consultarPago(paymentId);
        return procesarVerificacion(pedido, pago);
    }

    public PedidoDtos.PedidoResponse procesarWebhook(String paymentId) {
        PagoDtos.PagoVerificado pago = pasarelaPago.consultarPago(paymentId);
        Pedido pedido = pedidoService.buscarPorCodigo(pago.externalReference());
        return procesarVerificacion(pedido, pago);
    }

    public PedidoDtos.PedidoResponse cancelarPendiente(AuthTokenService.TokenData actor, Long pedidoId) {
        return pedidoService.cancelarPagoPendiente(actor, pedidoId);
    }

    public PedidoDtos.PedidoResponse confirmarManual(AuthTokenService.TokenData actor, Long pedidoId, String referencia) {
        return pedidoService.confirmarPagoManual(actor, pedidoId, referencia);
    }

    public PedidoDtos.PedidoResponse reembolsar(AuthTokenService.TokenData actor, Long pedidoId) {
        if (actor.rol() != Rol.ADMIN) throw new SecurityException("Solo el administrador puede reembolsar pagos");
        Pedido pedido = pedidoService.obtenerPedidoAutorizado(actor, pedidoId);
        if (pedido.getEstadoPago() != EstadoPago.APROBADO) {
            throw new IllegalStateException("El pedido no tiene un pago aprobado reembolsable");
        }
        if (PedidoService.MERCADO_PAGO.equals(pedido.getMetodoPago())) {
            if (pedido.getPagoId() == null || pedido.getPagoId().isBlank()) {
                throw new IllegalStateException("El pedido no tiene un identificador de pago verificable");
            }
            pasarelaPago.reembolsar(pedido.getPagoId());
        }
        return pedidoService.registrarReembolso(actor, pedidoId);
    }

    private PedidoDtos.PedidoResponse procesarVerificacion(Pedido pedido, PagoDtos.PagoVerificado pago) {
        validarCorrespondencia(pedido, pago);
        String status = pago.status() == null ? "" : pago.status().toLowerCase(Locale.ROOT);
        if ("approved".equals(status) && pedido.getEstado() == EstadoPedido.CANCELADO) {
            if (pedido.getEstadoPago() == EstadoPago.REEMBOLSADO) {
                return DtoMapper.toPedidoResponse(pedido);
            }
            // Si el proveedor aprobó justo después de expirar/cancelar la reserva, se devuelve el dinero
            // y se conserva la reversión de stock/cupón realizada localmente.
            pasarelaPago.reembolsar(pago.id());
            return pedidoService.registrarReembolsoSistema(pedido.getId(), pago);
        }
        return switch (status) {
            case "approved" -> confirmarPagoAprobado(pedido, pago);
            case "rejected", "cancelled" -> pedidoService.registrarPagoNoAprobado(pedido.getId(), pago);
            case "refunded", "charged_back" -> pedidoService.registrarReembolsoSistema(pedido.getId(), pago);
            case "pending", "in_process", "in_mediation" -> DtoMapper.toPedidoResponse(pedido);
            default -> throw new IllegalStateException("Estado de pago no reconocido por el servidor");
        };
    }

    private void validarPedidoParaPago(Pedido pedido) {
        if (!PedidoService.MERCADO_PAGO.equals(pedido.getMetodoPago())) throw new IllegalArgumentException("El pedido no utiliza Mercado Pago");
        if (pedido.getEstado() == EstadoPedido.CANCELADO) throw new IllegalStateException("El pedido está cancelado");
        if (pedido.getEstadoPago() == EstadoPago.APROBADO) throw new IllegalStateException("El pedido ya fue pagado");
        if (pedido.getEstadoPago() != EstadoPago.PENDIENTE) throw new IllegalStateException("El pedido ya no admite pagos");
        if (pedido.getFechaExpiracionPago() != null && pedido.getFechaExpiracionPago().isBefore(java.time.LocalDateTime.now())) {
            pedidoService.expirarReservasPendientes();
            throw new IllegalStateException("La reserva de pago expiró. Vuelve a crear el pedido.");
        }
    }

    private PedidoDtos.PedidoResponse confirmarPagoAprobado(Pedido pedido, PagoDtos.PagoVerificado pago) {
        try {
            return pedidoService.confirmarPagoMercadoPago(pedido.getId(), pago);
        } catch (PagoDuplicadoException ex) {
            // Dos preferencias pueden ser pagadas casi al mismo tiempo. El bloqueo del pedido
            // garantiza que solo una quede asociada; la segunda se devuelve inmediatamente.
            pasarelaPago.reembolsar(pago.id());
            return DtoMapper.toPedidoResponse(pedidoService.buscarPorCodigo(pedido.getCodigo()));
        }
    }

    private void validarCorrespondencia(Pedido pedido, PagoDtos.PagoVerificado pago) {
        if (pago == null || pago.id() == null) throw new IllegalStateException("Mercado Pago no devolvió un pago verificable");
        if (!pedido.getCodigo().equals(pago.externalReference())) {
            throw new SecurityException("La referencia del pago no corresponde al pedido");
        }
        if ("approved".equalsIgnoreCase(pago.status())) {
            BigDecimal monto = pago.transactionAmount();
            if (monto == null || monto.subtract(pedido.getTotal()).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw new SecurityException("El monto verificado no corresponde al pedido");
            }
        }
    }

    private static String sinBarraFinal(String valor) {
        if (valor == null) return "";
        return valor.trim().replaceAll("/+$", "");
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
