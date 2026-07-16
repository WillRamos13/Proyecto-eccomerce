package com.fastmarket.api.pattern;

import com.fastmarket.api.exception.AutenticacionException;
import com.fastmarket.api.service.MercadoPagoWebhookSecurityService;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MercadoPagoWebhookSecurityServiceTest {

    @Test
    void aceptaFirmaHmacValida() throws Exception {
        String secret = "secret-webhook-super-seguro-123456";
        String dataId = "123456789";
        String requestId = "req-abc";
        String timestamp = "1704908010";
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        String signature = "ts=" + timestamp + ",v1=" + hmac(secret, manifest);

        MercadoPagoWebhookSecurityService service = new MercadoPagoWebhookSecurityService(secret);
        assertDoesNotThrow(() -> service.validar(signature, requestId, dataId));
    }

    @Test
    void rechazaFirmaAlterada() {
        MercadoPagoWebhookSecurityService service = new MercadoPagoWebhookSecurityService(
                "secret-webhook-super-seguro-123456"
        );
        assertThrows(AutenticacionException.class,
                () -> service.validar("ts=1704908010,v1=incorrecta", "req-abc", "123456789"));
    }

    private String hmac(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format("%02x", b & 0xff));
        return hex.toString();
    }
}
