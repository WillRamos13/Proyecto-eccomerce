package com.fastmarket.api.service;

import com.fastmarket.api.exception.AutenticacionException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Valida la firma HMAC enviada por Mercado Pago antes de procesar el evento. */
@Service
public class MercadoPagoWebhookSecurityService {
    private final String webhookSecret;

    public MercadoPagoWebhookSecurityService(
            @Value("${mercadopago.webhook-secret:}") String webhookSecret
    ) {
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    public void validar(String xSignature, String xRequestId, String dataId) {
        // Permite desarrollo local sin credenciales. StartupSecurityCheck exige
        // la clave cuando Mercado Pago está activo en el perfil prod.
        if (webhookSecret.isBlank()) return;

        if (vacio(xSignature) || vacio(dataId)) {
            throw new AutenticacionException("Webhook de Mercado Pago sin firma verificable");
        }

        try {
            WebhookSignatureValidator.validate(xSignature, xRequestId, dataId, webhookSecret);
        } catch (Exception ex) {
            throw new AutenticacionException("Firma de webhook de Mercado Pago inválida");
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
