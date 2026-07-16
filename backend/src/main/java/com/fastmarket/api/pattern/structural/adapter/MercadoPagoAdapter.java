package com.fastmarket.api.pattern.structural.adapter;

import com.fastmarket.api.dto.PagoDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Patrón Adapter: traduce el puerto interno al contrato HTTP de Mercado Pago. */
@Component
public class MercadoPagoAdapter implements PasarelaPago {
    private final RestTemplate restTemplate;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.base-url:https://api.mercadopago.com}")
    private String baseUrl;

    public MercadoPagoAdapter() {
        this(new RestTemplate());
    }

    MercadoPagoAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public PagoDtos.PreferenciaCreada crearPreferencia(PagoDtos.PreferenciaPago solicitud) {
        validarConfiguracion();
        if (solicitud == null || solicitud.monto() == null || solicitud.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monto de pago inválido");
        }

        Map<String, Object> item = new HashMap<>();
        item.put("title", solicitud.descripcion());
        item.put("quantity", 1);
        item.put("currency_id", "PEN");
        item.put("unit_price", solicitud.monto());

        Map<String, Object> payload = new HashMap<>();
        payload.put("items", List.of(item));
        if (solicitud.correoPagador() != null && !solicitud.correoPagador().isBlank()) {
            payload.put("payer", Map.of("email", solicitud.correoPagador().trim()));
        }
        payload.put("external_reference", solicitud.codigoPedido());
        payload.put("statement_descriptor", "FastMarket");
        payload.put("auto_return", "approved");
        payload.put("back_urls", Map.of(
                "success", solicitud.successUrl(),
                "failure", solicitud.failureUrl(),
                "pending", solicitud.pendingUrl()
        ));
        if (solicitud.notificationUrl() != null && !solicitud.notificationUrl().isBlank()) {
            payload.put("notification_url", solicitud.notificationUrl());
        }
        if (solicitud.fechaExpiracion() != null) {
            payload.put("expires", true);
            payload.put("expiration_date_to", solicitud.fechaExpiracion()
                    .atZone(ZoneId.of("America/Lima")).toOffsetDateTime().toString());
        }

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/checkout/preferences",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers()),
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> body = validarRespuesta(response, "No se pudo crear la preferencia de Mercado Pago");
        return new PagoDtos.PreferenciaCreada(
                nombre(), texto(body.get("id")), texto(body.get("status")),
                texto(body.get("init_point")), texto(body.get("sandbox_init_point"))
        );
    }

    @Override
    public PagoDtos.PagoVerificado consultarPago(String paymentId) {
        validarConfiguracion();
        String idSeguro = idSeguro(paymentId);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/v1/payments/" + idSeguro,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> body = validarRespuesta(response, "No se pudo verificar el pago en Mercado Pago");
        String preferenceId = null;
        if (body.get("metadata") instanceof Map<?, ?> metadata) {
            preferenceId = texto(metadata.get("preference_id"));
        }
        return new PagoDtos.PagoVerificado(
                texto(body.get("id")), texto(body.get("status")), texto(body.get("external_reference")),
                decimal(body.get("transaction_amount")), preferenceId, fecha(body.get("date_approved"))
        );
    }

    @Override
    public void reembolsar(String paymentId) {
        validarConfiguracion();
        String idSeguro = idSeguro(paymentId);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/v1/payments/" + idSeguro + "/refunds",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers()),
                new ParameterizedTypeReference<>() {}
        );
        validarRespuesta(response, "Mercado Pago no pudo procesar el reembolso");
    }

    @Override
    public String nombre() {
        return "Mercado Pago";
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private void validarConfiguracion() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Mercado Pago no está configurado en el servidor");
        }
    }

    private String idSeguro(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El identificador de pago es obligatorio");
        return URLEncoder.encode(id.trim(), StandardCharsets.UTF_8);
    }

    private Map<String, Object> validarRespuesta(ResponseEntity<Map<String, Object>> response, String mensaje) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) throw new IllegalStateException(mensaje);
        return response.getBody();
    }

    private String texto(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return null;
        try { return new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException ex) { return null; }
    }

    private LocalDateTime fecha(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        String raw = String.valueOf(value);
        try { return OffsetDateTime.parse(raw).toLocalDateTime(); }
        catch (Exception ignored) {
            try { return LocalDateTime.parse(raw); }
            catch (Exception ignoredAgain) { return null; }
        }
    }
}
