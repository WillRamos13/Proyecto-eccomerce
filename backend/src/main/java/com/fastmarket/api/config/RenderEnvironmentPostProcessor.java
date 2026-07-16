package com.fastmarket.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte RENDER_EXTERNAL_HOSTNAME en las URLs HTTPS públicas que necesita
 * FastMarket. Evita codificar el subdominio generado por Render en el proyecto.
 */
public class RenderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String hostname = limpiarHostname(environment.getProperty("RENDER_EXTERNAL_HOSTNAME"));
        if (hostname.isBlank()) return;

        String publicUrl = "https://" + hostname;
        Map<String, Object> props = new LinkedHashMap<>();

        if (vacio(environment.getProperty("FRONTEND_BASE_URL"))) {
            props.put("app.frontend.base-url", publicUrl);
        }
        if (vacio(environment.getProperty("BACKEND_PUBLIC_URL"))) {
            props.put("app.backend.public-url", publicUrl);
        }
        if (vacio(environment.getProperty("CORS_ALLOWED_ORIGIN_PATTERNS"))) {
            props.put("app.cors.allowed-origin-patterns", publicUrl);
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("fastmarketRenderPublicUrl", props)
            );
        }
    }

    private String limpiarHostname(String value) {
        if (value == null) return "";
        return value.trim()
                .replaceFirst("^https?://", "")
                .replaceAll("/+$", "");
    }

    private boolean vacio(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
