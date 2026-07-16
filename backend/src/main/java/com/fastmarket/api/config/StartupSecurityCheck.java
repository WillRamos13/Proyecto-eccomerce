package com.fastmarket.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupSecurityCheck implements ApplicationRunner {
    private static final Logger LOGGER = Logger.getLogger(StartupSecurityCheck.class.getName());

    private final Environment environment;

    @Value("${app.auth.secret:}")
    private String authSecret;
    @Value("${app.admin.password:}")
    private String adminPassword;
    @Value("${app.cors.allowed-origin-patterns:}")
    private String corsOrigins;
    @Value("${mercadopago.access-token:}")
    private String mercadoPagoToken;
    @Value("${mercadopago.webhook-secret:}")
    private String mercadoPagoWebhookSecret;
    @Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;
    @Value("${app.backend.public-url:}")
    private String backendPublicUrl;

    public StartupSecurityCheck(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean produccion = Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);

        if (esInseguro(authSecret, 32, "no_secret", "change_me", "cambiar", "reemplazar")) {
            throw new IllegalStateException("APP_AUTH_SECRET es obligatorio y debe tener al menos 32 caracteres aleatorios");
        }

        if (esInseguro(adminPassword, 12, "admin123", "password", "123456", "cambia_esta", "reemplazar")) {
            if (produccion) {
                throw new IllegalStateException("ADMIN_PASSWORD es obligatorio y no puede usar una clave predecible en producción");
            }
            LOGGER.warning("ADMIN_PASSWORD no está configurado de forma segura; no se creará/restablecerá el administrador.");
        }

        if (corsOrigins != null && corsOrigins.contains("*")) {
            if (produccion) throw new IllegalStateException("CORS no puede contener '*' en producción");
            LOGGER.warning("CORS contiene '*'. Limita los orígenes antes de publicar.");
        }

        if (produccion && mercadoPagoToken != null && !mercadoPagoToken.isBlank()) {
            if (!esHttps(frontendBaseUrl) || !esHttps(backendPublicUrl)) {
                throw new IllegalStateException("FRONTEND_BASE_URL y BACKEND_PUBLIC_URL deben usar HTTPS cuando Mercado Pago está activo en producción");
            }
            if (esInseguro(mercadoPagoWebhookSecret, 16, "change_me", "cambiar", "reemplazar")) {
                throw new IllegalStateException("MERCADOPAGO_WEBHOOK_SECRET es obligatorio cuando Mercado Pago está activo en producción");
            }
        }
    }

    private boolean esInseguro(String valor, int longitudMinima, String... patronesPeligrosos) {
        String seguro = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        if (seguro.length() < longitudMinima) return true;
        for (String patron : patronesPeligrosos) {
            if (seguro.contains(patron.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private boolean esHttps(String valor) {
        return valor != null && valor.trim().toLowerCase(Locale.ROOT).startsWith("https://");
    }
}
