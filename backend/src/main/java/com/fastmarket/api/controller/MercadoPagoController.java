package com.fastmarket.api.controller;

import com.fastmarket.api.dto.PagoDtos;
import com.fastmarket.api.dto.PedidoDtos;
import com.fastmarket.api.pattern.structural.facade.ComercioElectronicoFacade;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.MercadoPagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class MercadoPagoController {
    private final ComercioElectronicoFacade comercioElectronicoFacade;
    private final MercadoPagoService mercadoPagoService;
    private final AuthTokenService authTokenService;

    public MercadoPagoController(
            ComercioElectronicoFacade comercioElectronicoFacade,
            MercadoPagoService mercadoPagoService,
            AuthTokenService authTokenService
    ) {
        this.comercioElectronicoFacade = comercioElectronicoFacade;
        this.mercadoPagoService = mercadoPagoService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/pedidos/{pedidoId}/preferencia")
    public PagoDtos.PreferenciaCreada crearPreferencia(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pedidoId
    ) {
        AuthTokenService.TokenData actor = authTokenService.validar(authorization);
        return comercioElectronicoFacade.crearPreferenciaPago(actor, pedidoId);
    }

    @PostMapping("/pedidos/{pedidoId}/confirmar")
    public PedidoDtos.PedidoResponse confirmarRetorno(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pedidoId,
            @RequestParam String paymentId
    ) {
        AuthTokenService.TokenData actor = authTokenService.validar(authorization);
        return mercadoPagoService.confirmarRetorno(actor, pedidoId, paymentId);
    }

    @PostMapping("/pedidos/{pedidoId}/cancelar")
    public PedidoDtos.PedidoResponse cancelarPendiente(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pedidoId
    ) {
        AuthTokenService.TokenData actor = authTokenService.validar(authorization);
        return mercadoPagoService.cancelarPendiente(actor, pedidoId);
    }

    @PutMapping("/pedidos/{pedidoId}/manual")
    public PedidoDtos.PedidoResponse confirmarManual(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pedidoId,
            @Valid @RequestBody PagoDtos.PagoManualRequest request
    ) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdmin(authorization);
        return mercadoPagoService.confirmarManual(actor, pedidoId, request.referencia());
    }

    @PostMapping("/pedidos/{pedidoId}/reembolsar")
    public PedidoDtos.PedidoResponse reembolsar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pedidoId
    ) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdmin(authorization);
        return mercadoPagoService.reembolsar(actor, pedidoId);
    }

    /**
     * El webhook no confía en el contenido recibido: solo extrae el ID y consulta
     * nuevamente a Mercado Pago usando las credenciales privadas del servidor.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestParam Map<String, String> params,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String tipo = params.getOrDefault("type", params.get("topic"));
        if (tipo == null && body != null) tipo = texto(body.get("type"));
        if (tipo != null && !tipo.equalsIgnoreCase("payment")) {
            return ResponseEntity.ok(Map.of("procesado", false, "motivo", "notificación no relacionada con pagos"));
        }

        String paymentId = params.get("data.id");
        if (paymentId == null) paymentId = params.get("id");
        if (paymentId == null && body != null && body.get("data") instanceof Map<?, ?> data) {
            paymentId = texto(data.get("id"));
        }
        if (paymentId == null || paymentId.isBlank()) throw new IllegalArgumentException("Notificación sin identificador de pago");

        PedidoDtos.PedidoResponse pedido = mercadoPagoService.procesarWebhook(paymentId);
        return ResponseEntity.ok(Map.of("procesado", true, "pedidoId", pedido.id(), "estadoPago", pedido.estadoPago().name()));
    }

    private String texto(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
