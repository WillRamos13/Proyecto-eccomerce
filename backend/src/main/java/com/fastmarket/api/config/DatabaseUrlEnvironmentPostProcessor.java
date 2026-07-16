package com.fastmarket.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String springUrl = trim(environment.getProperty("SPRING_DATASOURCE_URL"));
        String databaseUrl = trim(environment.getProperty("DATABASE_URL"));

        Map<String, Object> props = new HashMap<>();
        if (!springUrl.isBlank()) {
            props.put("spring.datasource.url", normalizarJdbcUrl(springUrl));
        } else if (!databaseUrl.isBlank()) {
            convertirDatabaseUrl(databaseUrl, props);
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("fastmarketDatabaseUrlNormalizer", props));
        }
    }

    private void convertirDatabaseUrl(String databaseUrl, Map<String, Object> props) {
        if (databaseUrl.startsWith("jdbc:")) {
            props.put("spring.datasource.url", databaseUrl);
            return;
        }
        try {
            URI uri = URI.create(databaseUrl);
            if (!"postgresql".equalsIgnoreCase(uri.getScheme()) && !"postgres".equalsIgnoreCase(uri.getScheme())) {
                props.put("spring.datasource.url", normalizarJdbcUrl(databaseUrl));
                return;
            }

            String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                    + (uri.getPath() == null ? "" : uri.getPath());
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl += "?" + uri.getQuery();
            }
            props.put("spring.datasource.url", jdbcUrl);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] partes = userInfo.split(":", 2);
                if (trim(System.getenv("SPRING_DATASOURCE_USERNAME")).isBlank()) props.put("spring.datasource.username", decode(partes[0]));
                if (partes.length > 1 && trim(System.getenv("SPRING_DATASOURCE_PASSWORD")).isBlank()) props.put("spring.datasource.password", decode(partes[1]));
            }
        } catch (RuntimeException ex) {
            props.put("spring.datasource.url", normalizarJdbcUrl(databaseUrl));
        }
    }

    private String normalizarJdbcUrl(String url) {
        if (url == null || url.isBlank()) return "";
        if (url.startsWith("jdbc:")) return url;
        if (url.startsWith("postgresql://")) return "jdbc:" + url;
        if (url.startsWith("postgres://")) return "jdbc:postgresql://" + url.substring("postgres://".length());
        return url;
    }

    private String decode(String value) {
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
